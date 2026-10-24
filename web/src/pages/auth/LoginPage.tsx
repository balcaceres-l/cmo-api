import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  HospitalLogoIcon,
  UserIcon,
  LockIcon,
  EyeIcon,
  EyeOffIcon,
  ShieldCheckIcon,
} from '../../components/Icons';

export const LoginPage: React.FC = () => {
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!usuario.trim() || !password) {
      setErrorMessage('Por favor ingresa tu usuario y contraseña.');
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login({ usuario: usuario.trim(), password }, remember);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const error = err as Error;
      setErrorMessage(error.message || 'No fue posible iniciar sesión. Verifica tus credenciales.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoLogin = () => {
    setUsuario('admin');
    setPassword('Hospital2026!');
    setErrorMessage(null);
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at 50% 20%, #0d2a21 0%, #061510 100%)',
        padding: '24px',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background Decorative Rings */}
      <div
        style={{
          position: 'absolute',
          width: '600px',
          height: '600px',
          borderRadius: '50%',
          border: '1px solid rgba(34, 197, 94, 0.08)',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          width: '900px',
          height: '900px',
          borderRadius: '50%',
          border: '1px solid rgba(34, 197, 94, 0.04)',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          pointerEvents: 'none',
        }}
      />

      {/* Main Card */}
      <div
        style={{
          width: '100%',
          maxWidth: '440px',
          background: '#ffffff',
          borderRadius: '20px',
          padding: '40px 36px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(255, 255, 255, 0.1)',
          position: 'relative',
          zIndex: 1,
        }}
      >
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div
            style={{
              display: 'inline-flex',
              padding: '10px',
              borderRadius: '20px',
              background: '#eaf5f1',
              boxShadow: '0 4px 14px rgba(11, 93, 75, 0.15)',
              marginBottom: '16px',
            }}
          >
            <HospitalLogoIcon size={52} />
          </div>
          <h1
            style={{
              fontSize: '1.45rem',
              fontWeight: 800,
              color: '#073f33',
              margin: '0 0 6px 0',
              letterSpacing: '-0.02em',
            }}
          >
            HOSPITAL CMO
          </h1>
          <p
            style={{
              color: '#526b60',
              fontSize: '0.86rem',
              margin: 0,
              fontWeight: 500,
            }}
          >
            Sistema Integrado Hospitalario — Panel Central
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div
            style={{
              background: '#fef2f2',
              border: '1px solid #fecaca',
              color: '#991b1b',
              padding: '12px 14px',
              borderRadius: '10px',
              fontSize: '0.84rem',
              lineHeight: 1.45,
              marginBottom: '20px',
              display: 'flex',
              gap: '8px',
              alignItems: 'flex-start',
            }}
          >
            <span style={{ fontSize: '1rem', lineHeight: 1 }}>⚠️</span>
            <div style={{ flex: 1 }}>{errorMessage}</div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '18px' }}>
          <div>
            <label
              htmlFor="usuario-input"
              style={{
                display: 'block',
                fontSize: '0.82rem',
                fontWeight: 700,
                color: '#13221c',
                marginBottom: '6px',
              }}
            >
              Usuario Administrativo
            </label>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                background: '#f8faf9',
                border: '1.5px solid #d1dcd6',
                borderRadius: '10px',
                padding: '0 12px',
                transition: 'border-color 0.15s ease',
              }}
              onFocus={(e) => (e.currentTarget.style.borderColor = '#0b5d4b')}
              onBlur={(e) => (e.currentTarget.style.borderColor = '#d1dcd6')}
            >
              <span style={{ color: '#849a90', marginRight: '8px', display: 'flex' }}>
                <UserIcon size={18} />
              </span>
              <input
                id="usuario-input"
                type="text"
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
                placeholder="Ej. admin"
                autoComplete="username"
                style={{
                  width: '100%',
                  border: 'none',
                  background: 'transparent',
                  padding: '12px 0',
                  fontSize: '0.92rem',
                  color: '#13221c',
                  outline: 'none',
                  fontFamily: 'inherit',
                }}
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="password-input"
              style={{
                display: 'block',
                fontSize: '0.82rem',
                fontWeight: 700,
                color: '#13221c',
                marginBottom: '6px',
              }}
            >
              Contraseña
            </label>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                background: '#f8faf9',
                border: '1.5px solid #d1dcd6',
                borderRadius: '10px',
                padding: '0 12px',
                transition: 'border-color 0.15s ease',
              }}
              onFocus={(e) => (e.currentTarget.style.borderColor = '#0b5d4b')}
              onBlur={(e) => (e.currentTarget.style.borderColor = '#d1dcd6')}
            >
              <span style={{ color: '#849a90', marginRight: '8px', display: 'flex' }}>
                <LockIcon size={18} />
              </span>
              <input
                id="password-input"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Ingresa tu contraseña"
                autoComplete="current-password"
                style={{
                  width: '100%',
                  border: 'none',
                  background: 'transparent',
                  padding: '12px 0',
                  fontSize: '0.92rem',
                  color: '#13221c',
                  outline: 'none',
                  fontFamily: 'inherit',
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  background: 'none',
                  border: 'none',
                  padding: '4px',
                  color: '#849a90',
                  cursor: 'pointer',
                  display: 'flex',
                }}
              >
                {showPassword ? <EyeOffIcon size={18} /> : <EyeIcon size={18} />}
              </button>
            </div>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.82rem',
            }}
          >
            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                color: '#526b60',
                cursor: 'pointer',
              }}
            >
              <input
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
                style={{ accentColor: '#0b5d4b' }}
              />
              Recordar en este equipo
            </label>
            <span
              style={{
                color: '#0b5d4b',
                fontWeight: 600,
                fontSize: '0.78rem',
              }}
            >
              Rol: ADMINISTRADOR
            </span>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              marginTop: '4px',
              padding: '14px',
              borderRadius: '10px',
              border: 'none',
              background: isSubmitting
                ? '#526b60'
                : 'linear-gradient(135deg, #0b5d4b 0%, #158066 100%)',
              color: '#ffffff',
              fontSize: '0.95rem',
              fontWeight: 700,
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              boxShadow: '0 4px 14px rgba(11, 93, 75, 0.35)',
              transition: 'all 0.2s ease',
            }}
          >
            {isSubmitting ? 'Verificando...' : 'Iniciar Sesión'}
          </button>
        </form>

        {/* Demo Fast Login Button */}
        <div
          style={{
            marginTop: '24px',
            paddingTop: '20px',
            borderTop: '1px solid #e2ece7',
            textAlign: 'center',
          }}
        >
          <button
            type="button"
            onClick={handleDemoLogin}
            style={{
              background: '#eaf5f1',
              border: '1px dashed #0b5d4b',
              color: '#0b5d4b',
              borderRadius: '8px',
              padding: '8px 16px',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
              width: '100%',
              transition: 'background 0.15s ease',
            }}
          >
            ⚡ Cargar credenciales Demo (admin)
          </button>
        </div>

        {/* Security Policy Badge */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
            marginTop: '20px',
            fontSize: '0.72rem',
            color: '#849a90',
          }}
        >
          <ShieldCheckIcon size={14} color="#0b5d4b" />
          <span>Acceso seguro protegido por Laravel Sanctum</span>
        </div>
      </div>
    </div>
  );
};
