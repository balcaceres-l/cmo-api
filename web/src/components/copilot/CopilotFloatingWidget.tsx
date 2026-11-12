import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getToken } from '../../api/client';

interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  timestamp: string;
}

interface ParsedAlert {
  level: 'CRITICO' | 'ADVERTENCIA' | 'OPTIMO' | 'INFO' | null;
  cleanText: string;
}

function parseAlertTag(rawText: string): ParsedAlert {
  const match = rawText.match(/^\[(CRITICO|ADVERTENCIA|OPTIMO|INFO)\]\s*/i);
  if (match) {
    const level = match[1].toUpperCase() as 'CRITICO' | 'ADVERTENCIA' | 'OPTIMO' | 'INFO';
    const cleanText = rawText.replace(match[0], '');
    return { level, cleanText };
  }
  return { level: null, cleanText: rawText };
}

function renderSafeMarkdown(text: string) {
  const lines = text.split('\n');
  return lines.map((line, lineIdx) => {
    const parts = line.split(/(\*\*.*?\*\*)/g);
    return (
      <span key={lineIdx} style={{ display: 'block', minHeight: line.trim() === '' ? '0.6em' : undefined }}>
        {parts.map((part, partIdx) => {
          if (part.startsWith('**') && part.endsWith('**')) {
            return <strong key={partIdx}>{part.slice(2, -2)}</strong>;
          }
          return part;
        })}
      </span>
    );
  });
}

const STORAGE_MESSAGES_KEY = 'cmo_copilot_session_messages';
const STORAGE_OPEN_KEY = 'cmo_copilot_session_is_open';

export const CopilotFloatingWidget: React.FC = () => {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState<boolean>(() => {
    try {
      return sessionStorage.getItem(STORAGE_OPEN_KEY) === 'true';
    } catch {
      return false;
    }
  });

  const getWelcomeMessage = (): ChatMessage => ({
    id: 'welcome',
    sender: 'ai',
    text: `¡Hola ${user?.nombre_completo || 'Administrador'}! Soy el Copiloto Operativo del Hospital CMO.\n\nTengo acceso en **tiempo real y en modo solo lectura** al estado del hospital (camas, ambulancia, insumos críticos y censo).\n\n¿En qué puedo asistirle hoy?`,
    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  });

  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_MESSAGES_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch (e) {
      console.warn('Error al cargar historial de chat de sessionStorage:', e);
    }
    return [getWelcomeMessage()];
  });

  const [inputPrompt, setInputPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Guardar mensajes en sessionStorage
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_MESSAGES_KEY, JSON.stringify(messages));
    } catch (e) {
      console.warn('Error al guardar historial en sessionStorage:', e);
    }
  }, [messages]);

  // Guardar estado abierto/cerrado en sessionStorage
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_OPEN_KEY, String(isOpen));
    } catch (e) {
      console.warn('Error al guardar estado abierto en sessionStorage:', e);
    }
  }, [isOpen]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isLoading, isOpen]);

  // Restricción: Solo usuarios con rol ADMINISTRADOR
  if (user?.rol !== 'ADMINISTRADOR') {
    return null;
  }

  const quickPrompts = [
    { label: '🛏️ Camas', prompt: '¿Cómo está la ocupación de camas en las plantas 2 y 3 del hospital?' },
    { label: '🚑 Ambulancia', prompt: '¿Cuál es el estado actual de la ambulancia y los servicios del mes?' },
    { label: '📦 Stock Crítico', prompt: '¿Tenemos recursos o medicamentos bajo el stock mínimo de inventario?' },
    { label: '🧪 Laboratorio', prompt: '¿Cuántas órdenes de laboratorio tenemos pendientes o en proceso?' },
    { label: '👥 Personal', prompt: '¿Cuántos colaboradores activos y pacientes registrados hay en el sistema?' },
  ];

  const handleSend = async (textToSend?: string) => {
    const text = (textToSend || inputPrompt).trim();
    if (!text || isLoading) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputPrompt('');
    setIsLoading(true);
    setErrorMsg(null);

    // Historial para contexto conversacional
    const historial = messages
      .filter((m) => m.id !== 'welcome')
      .slice(-8)
      .map((m) => ({
        role: m.sender === 'user' ? 'user' : 'assistant',
        content: m.text,
      }));

    const token = getToken();
    const webhookUrl =
      (window as unknown as { __ENV__?: { VITE_N8N_COPILOT_URL?: string } }).__ENV__?.VITE_N8N_COPILOT_URL ||
      import.meta.env.VITE_N8N_COPILOT_URL ||
      'http://192.168.110.200:5678/webhook/admin-copilot';

    try {
      const response = await fetch(webhookUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          mensaje: text,
          token: token || '',
          historial,
        }),
      });

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error('Permisos insuficientes: La API institucional rechazó la autenticación.');
        }
        throw new Error(`Error en el servicio analítico (${response.status}).`);
      }

      const data = await response.json();
      const aiReply = data.respuesta || 'No se recibió respuesta del sistema analítico.';

      const aiMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: aiReply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err: unknown) {
      const e = err as Error;
      setErrorMsg(e.message || 'Error de conexión con el servicio analítico.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClearHistory = () => {
    const welcome = getWelcomeMessage();
    setMessages([welcome]);
    try {
      sessionStorage.removeItem(STORAGE_MESSAGES_KEY);
    } catch (e) {
      console.warn('Error al limpiar sessionStorage:', e);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <>
      {/* Botón Flotante Launcher (cuando está minimizado/cerrado) */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          title="Abrir Copiloto Operativo CMO"
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            width: '60px',
            height: '60px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #091a14 0%, #0d2b22 100%)',
            border: '2px solid #10b981',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.25)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'scale(1.08)';
            e.currentTarget.style.boxShadow = '0 12px 30px rgba(16, 185, 129, 0.35)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'scale(1)';
            e.currentTarget.style.boxShadow = '0 8px 24px rgba(0, 0, 0, 0.25)';
          }}
        >
          {/* Icono de Chat institucional con pulso verde */}
          <div style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <span
              style={{
                position: 'absolute',
                top: '-2px',
                right: '-2px',
                width: '10px',
                height: '10px',
                borderRadius: '50%',
                background: '#10b981',
                boxShadow: '0 0 8px #10b981',
                border: '2px solid #091a14',
              }}
            />
          </div>
        </button>
      )}

      {/* Ventana Flotante del Chat (cuando está abierta) */}
      {isOpen && (
        <div
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            width: '430px',
            maxWidth: 'calc(100vw - 32px)',
            height: '620px',
            maxHeight: 'calc(100vh - 48px)',
            background: '#ffffff',
            borderRadius: '20px',
            boxShadow: '0 16px 48px rgba(0, 0, 0, 0.25)',
            border: '1px solid #cbd5e1',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            zIndex: 9999,
            animation: 'fadeInUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
          }}
        >
          {/* Header del Widget */}
          <div
            style={{
              background: 'linear-gradient(135deg, #091a14 0%, #0d2b22 100%)',
              padding: '16px 20px',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid #1a4d3e',
              flexShrink: 0,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div
                style={{
                  width: '38px',
                  height: '38px',
                  borderRadius: '10px',
                  background: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 2px 8px rgba(16, 185, 129, 0.3)',
                }}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
                </svg>
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 800, color: '#f8fafc', letterSpacing: '-0.01em' }}>
                  Copiloto Operativo CMO
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px' }}>
                  <span
                    style={{
                      width: '7px',
                      height: '7px',
                      borderRadius: '50%',
                      background: '#10b981',
                      boxShadow: '0 0 6px #10b981',
                    }}
                  />
                  <span style={{ fontSize: '0.72rem', color: '#94a3b8' }}>Tiempo real · Solo lectura · Admin</span>
                </div>
              </div>
            </div>

            {/* Botones de Acción de Ventana */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              {/* Botón Limpiar Chat */}
              <button
                onClick={handleClearHistory}
                title="Limpiar conversación actual"
                style={{
                  background: 'rgba(255, 255, 255, 0.08)',
                  border: 'none',
                  borderRadius: '8px',
                  width: '32px',
                  height: '32px',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.16)';
                  e.currentTarget.style.color = '#ffffff';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.08)';
                  e.currentTarget.style.color = '#94a3b8';
                }}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>

              {/* Botón Minimizar */}
              <button
                onClick={() => setIsOpen(false)}
                title="Minimizar ventana"
                style={{
                  background: 'rgba(255, 255, 255, 0.08)',
                  border: 'none',
                  borderRadius: '8px',
                  width: '32px',
                  height: '32px',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.16)';
                  e.currentTarget.style.color = '#ffffff';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.08)';
                  e.currentTarget.style.color = '#94a3b8';
                }}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M5 12h14" />
                </svg>
              </button>
            </div>
          </div>

          {/* Barra de Consultas Frecuentes (Pills) */}
          <div
            style={{
              padding: '8px 14px',
              background: '#f8fafc',
              borderBottom: '1px solid #e2e8f0',
              display: 'flex',
              gap: '6px',
              overflowX: 'auto',
              whiteSpace: 'nowrap',
              flexShrink: 0,
            }}
          >
            {quickPrompts.map((qp, idx) => (
              <button
                key={idx}
                disabled={isLoading}
                onClick={() => handleSend(qp.prompt)}
                style={{
                  background: '#ffffff',
                  border: '1px solid #cbd5e1',
                  borderRadius: '999px',
                  padding: '4px 10px',
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  color: '#334155',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  transition: 'all 0.15s ease',
                  flexShrink: 0,
                }}
                onMouseEnter={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.borderColor = '#059669';
                    e.currentTarget.style.color = '#059669';
                    e.currentTarget.style.background = '#f0fdf4';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isLoading) {
                    e.currentTarget.style.borderColor = '#cbd5e1';
                    e.currentTarget.style.color = '#334155';
                    e.currentTarget.style.background = '#ffffff';
                  }
                }}
              >
                {qp.label}
              </button>
            ))}
          </div>

          {/* Área de Mensajes */}
          <div
            style={{
              flex: 1,
              overflowY: 'auto',
              padding: '16px',
              background: '#ffffff',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
            }}
          >
            {messages.map((m) => {
              const isUser = m.sender === 'user';
              const { level, cleanText } = !isUser ? parseAlertTag(m.text) : { level: null, cleanText: m.text };

              const getBadgeConfig = () => {
                switch (level) {
                  case 'CRITICO':
                    return { label: '🚨 ALERTA CRÍTICA', bg: '#fef2f2', border: '#f87171', color: '#991b1b', accent: '3px solid #ef4444' };
                  case 'ADVERTENCIA':
                    return { label: '⚠️ ADVERTENCIA OPERATIVA', bg: '#fffbeb', border: '#fcd34d', color: '#92400e', accent: '3px solid #f59e0b' };
                  case 'OPTIMO':
                    return { label: '✅ ESTADO ÓPTIMO', bg: '#f0fdf4', border: '#86efac', color: '#166534', accent: '3px solid #10b981' };
                  case 'INFO':
                    return { label: 'ℹ️ REPORTE INSTITUCIONAL', bg: '#f0f9ff', border: '#7dd3fc', color: '#0369a1', accent: '3px solid #0284c7' };
                  default:
                    return null;
                }
              };

              const badgeConfig = getBadgeConfig();

              return (
                <div
                  key={m.id}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: isUser ? 'flex-end' : 'flex-start',
                    maxWidth: '90%',
                    alignSelf: isUser ? 'flex-end' : 'flex-start',
                  }}
                >
                  <div
                    style={{
                      padding: '10px 14px',
                      borderRadius: isUser ? '14px 14px 2px 14px' : '14px 14px 14px 2px',
                      background: isUser ? 'linear-gradient(135deg, #0b5d4b 0%, #158066 100%)' : '#f8fafc',
                      color: isUser ? '#ffffff' : '#1e293b',
                      fontSize: '0.85rem',
                      lineHeight: '1.45',
                      border: isUser ? 'none' : '1px solid #e2e8f0',
                      borderLeft: !isUser && badgeConfig ? badgeConfig.accent : isUser ? 'none' : '1px solid #e2e8f0',
                      boxShadow: isUser ? '0 2px 6px rgba(11, 93, 75, 0.2)' : '0 1px 3px rgba(0,0,0,0.02)',
                    }}
                  >
                    {!isUser && badgeConfig && (
                      <div
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          padding: '2px 8px',
                          borderRadius: '6px',
                          fontSize: '0.68rem',
                          fontWeight: 800,
                          background: badgeConfig.bg,
                          border: `1px solid ${badgeConfig.border}`,
                          color: badgeConfig.color,
                          marginBottom: '6px',
                          letterSpacing: '0.02em',
                        }}
                      >
                        {badgeConfig.label}
                      </div>
                    )}
                    <div>{renderSafeMarkdown(cleanText)}</div>
                  </div>
                  <span style={{ fontSize: '0.68rem', color: '#94a3b8', marginTop: '3px', padding: '0 4px' }}>
                    {isUser ? 'Tú' : 'Asistente CMO'} · {m.timestamp}
                  </span>
                </div>
              );
            })}

            {isLoading && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 14px', background: '#f8fafc', borderRadius: '12px', width: 'fit-content', border: '1px solid #e2e8f0' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#059669', animation: 'ping 1s infinite' }} />
                <span style={{ fontSize: '0.8rem', color: '#64748b', fontStyle: 'italic' }}>Consultando estado hospitalario...</span>
              </div>
            )}

            {errorMsg && (
              <div style={{ padding: '10px 12px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '10px', color: '#dc2626', fontSize: '0.78rem' }}>
                ⚠️ {errorMsg}
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Barra de Entrada Inferior */}
          <div
            style={{
              padding: '12px 14px',
              background: '#ffffff',
              borderTop: '1px solid #e2e8f0',
              flexShrink: 0,
            }}
          >
            <div
              style={{
                display: 'flex',
                gap: '8px',
                alignItems: 'flex-end',
                background: '#f8fafc',
                borderRadius: '12px',
                padding: '6px 10px',
                border: '1px solid #cbd5e1',
              }}
            >
              <textarea
                value={inputPrompt}
                onChange={(e) => setInputPrompt(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Escriba su consulta operativa..."
                rows={1}
                disabled={isLoading}
                style={{
                  flex: 1,
                  border: 'none',
                  background: 'transparent',
                  outline: 'none',
                  resize: 'none',
                  fontSize: '0.85rem',
                  fontFamily: 'inherit',
                  color: '#1e293b',
                  maxHeight: '80px',
                  lineHeight: '1.4',
                  padding: '4px 0',
                }}
              />
              <button
                disabled={!inputPrompt.trim() || isLoading}
                onClick={() => handleSend()}
                title="Enviar mensaje"
                style={{
                  background: inputPrompt.trim() && !isLoading ? 'linear-gradient(135deg, #059669 0%, #10b981 100%)' : '#e2e8f0',
                  color: inputPrompt.trim() && !isLoading ? '#ffffff' : '#94a3b8',
                  border: 'none',
                  borderRadius: '8px',
                  width: '32px',
                  height: '32px',
                  cursor: inputPrompt.trim() && !isLoading ? 'pointer' : 'not-allowed',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'all 0.15s ease',
                  flexShrink: 0,
                }}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
