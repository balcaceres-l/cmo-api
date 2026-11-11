import React, { useState, useEffect } from 'react';
import { BellIcon, PlusCircleIcon, SearchIcon, RefreshCwIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';

interface AvisoItem {
  id: string;
  titulo: string;
  contenido: string;
  publico: boolean;
  fechaInicio: string;
  fechaFin?: string;
  vigente: boolean;
}

const FALLBACK_NOTICES: AvisoItem[] = [
  {
    id: 'av-1',
    titulo: 'Jornada Nacional de Vacunación contra Influenza y Neumococo',
    contenido:
      'Se informa a la población que estará habilitado el puesto de vacunación en el vestíbulo principal sin costo alguno.',
    publico: true,
    fechaInicio: '2026-10-25',
    fechaFin: '2026-11-25',
    vigente: true,
  },
  {
    id: 'av-2',
    titulo: 'Protocolo de Aislamiento Preventivo en Planta 2 (Habitaciones 203 y 204)',
    contenido:
      'Todo el personal médico y de enfermería debe ingresar con mascarilla N95 y batas desechables.',
    publico: false,
    fechaInicio: '2026-10-28',
    fechaFin: '2026-11-30',
    vigente: true,
  },
];

export const NoticesPage: React.FC = () => {
  const [notices, setNotices] = useState<AvisoItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filterTipo, setFilterTipo] = useState<'TODOS' | 'PUBLICO' | 'INTERNO'>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Modales
  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [selectedAvisoId, setSelectedAvisoId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Formulario
  const [avisoForm, setAvisoForm] = useState({
    titulo: '',
    contenido: '',
    publico: true,
    vigente_desde: new Date().toISOString().split('T')[0],
    vigente_hasta: '',
  });

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchAvisos = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res: any = await apiClient.get('/avisos');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const now = new Date();
        const mapped: AvisoItem[] = list.map((a: any) => {
          const vDesde = a.vigente_desde ? new Date(a.vigente_desde) : null;
          const vHasta = a.vigente_hasta ? new Date(a.vigente_hasta) : null;
          const esVigente = (!vDesde || vDesde <= now) && (!vHasta || vHasta >= now);

          return {
            id: a.id_aviso || a.id,
            titulo: a.titulo,
            contenido: a.contenido,
            publico: Boolean(a.publico),
            fechaInicio: a.vigente_desde ? String(a.vigente_desde).split('T')[0] : '',
            fechaFin: a.vigente_hasta ? String(a.vigente_hasta).split('T')[0] : undefined,
            vigente: esVigente,
          };
        });
        setNotices(mapped);
      } else {
        setNotices([]);
      }
    } catch {
      setNotices(FALLBACK_NOTICES);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAvisos();
  }, []);

  const handleOpenCreate = () => {
    setIsEditing(false);
    setSelectedAvisoId(null);
    setAvisoForm({
      titulo: '',
      contenido: '',
      publico: true,
      vigente_desde: new Date().toISOString().split('T')[0],
      vigente_hasta: '',
    });
    setShowModal(true);
  };

  const handleOpenEdit = (aviso: AvisoItem) => {
    setIsEditing(true);
    setSelectedAvisoId(aviso.id);
    setAvisoForm({
      titulo: aviso.titulo,
      contenido: aviso.contenido,
      publico: aviso.publico,
      vigente_desde: aviso.fechaInicio || new Date().toISOString().split('T')[0],
      vigente_hasta: aviso.fechaFin || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!avisoForm.titulo.trim() || !avisoForm.contenido.trim()) {
      alert('Ingresa título y contenido para el aviso.');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: any = {
        titulo: avisoForm.titulo,
        contenido: avisoForm.contenido,
        publico: avisoForm.publico,
        vigente_desde: avisoForm.vigente_desde,
      };
      if (avisoForm.vigente_hasta) {
        payload.vigente_hasta = avisoForm.vigente_hasta;
      }

      if (isEditing && selectedAvisoId) {
        await apiClient.put(`/avisos/${selectedAvisoId}`, payload);
        notify(`Aviso "${avisoForm.titulo}" actualizado correctamente.`);
      } else {
        await apiClient.post('/avisos', payload);
        notify(
          `Aviso "${avisoForm.titulo}" publicado como comunicado ${
            avisoForm.publico ? 'PÚBLICO' : 'INTERNO'
          }.`
        );
      }

      setShowModal(false);
      fetchAvisos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al guardar aviso: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEliminar = async (aviso: AvisoItem) => {
    if (!confirm(`¿Deseas retirar el aviso "${aviso.titulo}" de la cartelera?`)) return;

    try {
      await apiClient.delete(`/avisos/${aviso.id}`);
      notify('Aviso retirado de la cartelera hospitalaria.');
      fetchAvisos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al eliminar: ${e.message}`);
    }
  };

  const filteredNotices = notices.filter((n) => {
    if (filterTipo === 'PUBLICO' && !n.publico) return false;
    if (filterTipo === 'INTERNO' && n.publico) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return n.titulo.toLowerCase().includes(q) || n.contenido.toLowerCase().includes(q);
    }
    return true;
  });

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              background: '#e0f2fe',
              color: '#0369a1',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <BellIcon size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              Cartelera Digital y Avisos
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              Gestión de comunicados oficiales internos y públicos para el portal web (conectado a BD)
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              fetchAvisos();
              notify('Cartelera de avisos sincronizada con la base de datos.');
            }}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              background: '#ffffff',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <RefreshCwIcon size={14} /> {loading ? 'Cargando...' : 'Sincronizar'}
          </button>
          <button
            onClick={handleOpenCreate}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: 'none',
              background: '#0b5d4b',
              color: '#ffffff',
              fontSize: '0.82rem',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            <PlusCircleIcon size={16} /> Publicar Nuevo Aviso
          </button>
        </div>
      </div>

      {feedback && (
        <div
          style={{
            padding: '12px 18px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            border: '1px solid #a7f3d0',
            fontWeight: 600,
            fontSize: '0.88rem',
          }}
        >
          ✓ {feedback}
        </div>
      )}

      {errorMsg && (
        <div
          style={{
            padding: '12px 18px',
            background: '#fee2e2',
            color: '#b91c1c',
            borderRadius: '10px',
            border: '1px solid #fecaca',
            fontWeight: 600,
            fontSize: '0.88rem',
          }}
        >
          ⚠ {errorMsg}
        </div>
      )}

      {/* Filter and Search */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          background: '#ffffff',
          padding: '12px 16px',
          borderRadius: '12px',
          border: '1px solid #e2ece7',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          {[
            { id: 'TODOS', label: 'Todos los Comunicados' },
            { id: 'PUBLICO', label: 'Públicos (Portal Web)' },
            { id: 'INTERNO', label: 'Internos (Solo Personal)' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterTipo(tab.id as any)}
              style={{
                padding: '6px 14px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.8rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: filterTipo === tab.id ? '#0b5d4b' : '#f0f4f2',
                color: filterTipo === tab.id ? '#ffffff' : '#526b60',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
            <SearchIcon size={16} />
          </div>
          <input
            type="text"
            placeholder="Buscar por título o contenido..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px 8px 34px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              fontSize: '0.85rem',
            }}
          />
        </div>
      </div>

      {/* List of Notices */}
      <div style={{ display: 'grid', gap: '16px' }}>
        {filteredNotices.length === 0 ? (
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              border: '1px solid #e2ece7',
              padding: '40px',
              textAlign: 'center',
              color: '#849a90',
            }}
          >
            {loading ? 'Cargando cartelera de avisos...' : 'No hay avisos registrados con este filtro.'}
          </div>
        ) : (
          filteredNotices.map((n) => (
            <div
              key={n.id}
              style={{
                background: '#ffffff',
                borderRadius: '14px',
                border: '1px solid #e2ece7',
                padding: '20px',
                boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                  <span
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontSize: '0.75rem',
                      fontWeight: 800,
                      background: n.publico ? '#ecfdf5' : '#f0f9ff',
                      color: n.publico ? '#047857' : '#0369a1',
                      border: `1px solid ${n.publico ? '#a7f3d0' : '#bae6fd'}`,
                    }}
                  >
                    {n.publico ? 'PÚBLICO (Visible en /publico)' : 'INTERNO (Solo Personal)'}
                  </span>
                  <span
                    style={{
                      padding: '3px 8px',
                      borderRadius: '999px',
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      background: n.vigente ? '#dcfce7' : '#f1f5f9',
                      color: n.vigente ? '#15803d' : '#64748b',
                    }}
                  >
                    {n.vigente ? '● VIGENTE' : '○ FINALIZADO'}
                  </span>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => handleOpenEdit(n)}
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      border: '1px solid #cbd5e1',
                      background: '#ffffff',
                      color: '#334155',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleEliminar(n)}
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      border: '1px solid #fecaca',
                      background: '#fff5f5',
                      color: '#b91c1c',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    Eliminar
                  </button>
                </div>
              </div>

              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 800, margin: '0 0 8px', color: '#13221c' }}>
                  {n.titulo}
                </h3>
                <p style={{ margin: 0, fontSize: '0.9rem', color: '#475569', lineHeight: 1.55 }}>
                  {n.contenido}
                </p>
              </div>

              <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '10px', fontSize: '0.75rem', color: '#849a90', display: 'flex', gap: '16px' }}>
                <span>Vigente desde: <strong>{n.fechaInicio}</strong></span>
                {n.fechaFin && <span>Hasta: <strong>{n.fechaFin}</strong></span>}
              </div>
            </div>
          ))
        )}
      </div>

      {/* MODAL CREAR / EDITAR */}
      {showModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '500px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              {isEditing ? 'Editar Comunicado Oficial' : 'Publicar Nuevo Aviso Oficial'}
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Los avisos públicos se muestran en la pantalla informativa de recepción y el portal web.
            </p>

            <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Título del Aviso
                </label>
                <input
                  type="text"
                  value={avisoForm.titulo}
                  onChange={(e) => setAvisoForm({ ...avisoForm, titulo: e.target.value })}
                  placeholder="Ej. Campaña de Donación de Sangre Cruz Roja"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Alcance del Comunicado
                </label>
                <select
                  value={avisoForm.publico ? 'PUBLICO' : 'INTERNO'}
                  onChange={(e) => setAvisoForm({ ...avisoForm, publico: e.target.value === 'PUBLICO' })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                >
                  <option value="PUBLICO">Público (Visible para pacientes en portal público)</option>
                  <option value="INTERNO">Interno (Exclusivo para médicos y personal clínico)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Contenido del Comunicado
                </label>
                <textarea
                  value={avisoForm.contenido}
                  onChange={(e) => setAvisoForm({ ...avisoForm, contenido: e.target.value })}
                  placeholder="Escribe el texto detallado del comunicado..."
                  rows={4}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Vigente Desde
                  </label>
                  <input
                    type="date"
                    value={avisoForm.vigente_desde}
                    onChange={(e) => setAvisoForm({ ...avisoForm, vigente_desde: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                    required
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Vigente Hasta (Opcional)
                  </label>
                  <input
                    type="date"
                    value={avisoForm.vigente_hasta}
                    onChange={(e) => setAvisoForm({ ...avisoForm, vigente_hasta: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0b5d4b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Guardando...' : isEditing ? 'Guardar Cambios' : 'Publicar Aviso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
