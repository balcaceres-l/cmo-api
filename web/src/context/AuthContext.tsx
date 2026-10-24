import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, LoginCredentials } from '../types/auth';
import { loginApi, getProfileApi, logoutApi } from '../api/auth';
import { getToken, removeToken, setOnUnauthorized } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginCredentials, remember?: boolean) => Promise<User>;
  logout: () => Promise<void>;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('cmo_auth_user') || sessionStorage.getItem('cmo_auth_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  });

  const [token, setTokenState] = useState<string | null>(() => getToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(async () => {
    try {
      await logoutApi();
    } catch {
      // Ignorar errores de red al cerrar sesión
    } finally {
      setUser(null);
      setTokenState(null);
      removeToken();
      localStorage.removeItem('cmo_auth_user');
      sessionStorage.removeItem('cmo_auth_user');
    }
  }, []);

  // Configurar interceptor 401
  useEffect(() => {
    setOnUnauthorized(() => {
      setUser(null);
      setTokenState(null);
      localStorage.removeItem('cmo_auth_user');
      sessionStorage.removeItem('cmo_auth_user');
    });
  }, []);

  // Cargar usuario inicial si existe token
  useEffect(() => {
    const initAuth = async () => {
      const currentToken = getToken();
      if (!currentToken) {
        setIsLoading(false);
        return;
      }

      try {
        const response = await getProfileApi();
        if (response.usuario.rol !== 'ADMINISTRADOR') {
          await logout();
          return;
        }
        setUser(response.usuario);
        setTokenState(currentToken);
        localStorage.setItem('cmo_auth_user', JSON.stringify(response.usuario));
      } catch {
        // Token inválido o expirado
        removeToken();
        setUser(null);
        setTokenState(null);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, [logout]);

  const login = async (credentials: LoginCredentials, remember = false): Promise<User> => {
    const res = await loginApi(credentials, remember);

    // Validación estricta de rol para Web (HU-24: Solo ADMINISTRADOR)
    if (res.usuario.rol !== 'ADMINISTRADOR') {
      removeToken();
      throw new Error(
        `Acceso restringido: El rol "${res.usuario.rol}" debe utilizar la aplicación móvil correspondiente (iOS o Android).`
      );
    }

    setUser(res.usuario);
    setTokenState(res.token);

    if (remember) {
      localStorage.setItem('cmo_auth_user', JSON.stringify(res.usuario));
    } else {
      sessionStorage.setItem('cmo_auth_user', JSON.stringify(res.usuario));
    }

    return res.usuario;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        isLoading,
        login,
        logout,
        setUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser utilizado dentro de un AuthProvider');
  }
  return context;
}
