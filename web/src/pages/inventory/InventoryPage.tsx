import React, { useState, useEffect } from 'react';
import { PackageIcon, SearchIcon, RefreshCwIcon, PlusCircleIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';

interface RecursoItem {
  id: string;
  codigo: string;
  nombre: string;
  categoria: 'Medicamento' | 'Insumo' | 'Equipo';
  ubicacion: string;
  stockActual: number;
  stockMinimo: number;
  unidad: string;
  updated_at?: string;
}

const FALLBACK_INVENTORY: RecursoItem[] = [
  {
    id: 'inv-1',
    codigo: 'REC-001',
    nombre: 'Solución Salina 0.9% (Hartman 500ml)',
    categoria: 'Medicamento',
    ubicacion: 'Farmacia Central / Bodega P2',
    stockActual: 18,
    stockMinimo: 35,
    unidad: 'Frasco 500ml',
  },
  {
    id: 'inv-2',
    codigo: 'REC-002',
    nombre: 'Catéter Venoso Periférico N° 18G',
    categoria: 'Insumo',
    ubicacion: 'Caja de Paro / Emergencias',
    stockActual: 12,
    stockMinimo: 30,
    unidad: 'Unidad',
  },
];

export const InventoryPage: React.FC = () => {
  const { user } = useAuth();
  const isOps = isJefeOperaciones(user);

  const [items, setItems] = useState<RecursoItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filterCat, setFilterCat] = useState<string>('TODAS');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Modales
  const [selectedItem, setSelectedItem] = useState<RecursoItem | null>(null);
  const [showMovModal, setShowMovModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Formulario de Movimiento
  const [movForm, setMovForm] = useState({
    tipo: 'ENTRADA' as 'ENTRADA' | 'SALIDA',
    cantidad: 1,
    motivo: '',
  });

  // Formulario Crear / Editar Recurso
  const [recursoForm, setRecursoForm] = useState({
    nombre: '',
    categoria: 'Medicamento' as 'Medicamento' | 'Insumo' | 'Equipo',
    unidad: 'Unidad',
    ubicacion: 'Farmacia Central',
    stock_minimo: 10,
    cantidad: 10,
  });

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchRecursos = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res: any = await apiClient.get('/recursos');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const mapped: RecursoItem[] = list.map((r: any) => ({
          id: r.id_recurso || r.id,
          codigo: r.id_recurso ? `REC-${String(r.id_recurso).slice(0, 6).toUpperCase()}` : `REC-${r.id}`,
          nombre: r.nombre,
          categoria: r.categoria,
          ubicacion: r.ubicacion || 'Bodega Central',
          stockActual: r.cantidad ?? 0,
          stockMinimo: r.stock_minimo ?? 0,
          unidad: r.unidad || 'Unidad',
          updated_at: r.updated_at,
        }));
        setItems(mapped);
      } else {
        setItems([]);
      }
    } catch {
      setItems(FALLBACK_INVENTORY);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecursos();
  }, []);

  // Registrar Movimiento (Entrada o Salida)
  const handleRegistrarMovimiento = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedItem || !movForm.motivo.trim() || movForm.cantidad <= 0) {
      alert('Ingresa una cantidad mayor a 0 y el motivo.');
      return;
    }

    if (movForm.tipo === 'SALIDA' && selectedItem.stockActual - movForm.cantidad < 0) {
      alert('Error de inventario (409): No se permiten salidas que dejen el stock en negativo.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.post(`/recursos/${selectedItem.id}/movimientos`, {
        tipo: movForm.tipo,
        cantidad: movForm.cantidad,
        motivo: movForm.motivo,
      });

      notify(
        `Movimiento registrado: ${movForm.tipo} de ${movForm.cantidad} ${selectedItem.unidad} para ${selectedItem.nombre}.`
      );
      setShowMovModal(false);
      setSelectedItem(null);
      setMovForm({ tipo: 'ENTRADA', cantidad: 1, motivo: '' });
      fetchRecursos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al registrar movimiento: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Crear Nuevo Recurso
  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recursoForm.nombre.trim()) {
      alert('Ingresa el nombre del recurso.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.post('/recursos', recursoForm);
      notify(`Recurso "${recursoForm.nombre}" creado exitosamente.`);
      setShowCreateModal(false);
      setRecursoForm({
        nombre: '',
        categoria: 'Medicamento',
        unidad: 'Unidad',
        ubicacion: 'Farmacia Central',
        stock_minimo: 10,
        cantidad: 10,
      });
      fetchRecursos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al crear recurso: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Editar Recurso
  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedItem || !recursoForm.nombre.trim()) return;

    setIsSubmitting(true);
    try {
      await apiClient.put(`/recursos/${selectedItem.id}`, {
        nombre: recursoForm.nombre,
        categoria: recursoForm.categoria,
        unidad: recursoForm.unidad,
        ubicacion: recursoForm.ubicacion,
        stock_minimo: recursoForm.stock_minimo,
      });
      notify(`Recurso "${recursoForm.nombre}" actualizado.`);
      setShowEditModal(false);
      setSelectedItem(null);
      fetchRecursos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al editar recurso: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Eliminar Recurso
  const handleDeleteRecurso = async (item: RecursoItem) => {
    if (!confirm(`¿Deseas eliminar el recurso "${item.nombre}"? Solo es posible si no tiene movimientos.`)) return;

    try {
      await apiClient.delete(`/recursos/${item.id}`);
      notify(`Recurso "${item.nombre}" eliminado del catálogo.`);
      fetchRecursos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`No se pudo eliminar: ${e.message}`);
    }
  };

  const filteredItems = items.filter((i) => {
    if (filterCat !== 'TODAS' && i.categoria !== filterCat) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        i.codigo.toLowerCase().includes(q) ||
        i.nombre.toLowerCase().includes(q) ||
        i.ubicacion.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const totalItems = items.length;
  const lowStockCount = items.filter((i) => i.stockActual <= i.stockMinimo).length;
  const medicamentosCount = items.filter((i) => i.categoria === 'Medicamento').length;

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
              background: '#ecfdf5',
              color: '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <PackageIcon size={24} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 800,
                  background: isOps ? '#0284c7' : '#0b5d4b',
                  color: '#ffffff',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em',
                }}
              >
                {isOps ? 'Control de Stock y Bodega' : 'Gestión de Catálogo Institucional'}
              </span>
            </div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              {isOps ? 'Control Físico de Insumos y Farmacia' : 'Catálogo de Insumos y Farmacia'}
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              {isOps
                ? 'Registro de entradas de abastecimiento y salidas a pisos/quirófano. Monitoreo de alertas de reposición.'
                : 'Definición técnica y alta de insumos en catálogo. Los movimientos físicos de almacén son operados por la Jefatura de Operaciones.'}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              fetchRecursos();
              notify('Inventario sincronizado con la base de datos.');
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
          {!isOps && (
            <button
              onClick={() => {
                setRecursoForm({
                  nombre: '',
                  categoria: 'Medicamento',
                  unidad: 'Unidad',
                  ubicacion: 'Farmacia Central',
                  stock_minimo: 10,
                  cantidad: 10,
                });
                setShowCreateModal(true);
              }}
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
              <PlusCircleIcon size={16} /> Nuevo Recurso
            </button>
          )}
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

      {/* KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '14px',
        }}
      >
        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
            Artículos en Catálogo
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#13221c', marginTop: '4px' }}>
            {totalItems}
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Medicamentos, insumos y equipos</span>
        </div>

        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#b91c1c', textTransform: 'uppercase' }}>
            Bajo Stock Crítico
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#dc2626', marginTop: '4px' }}>
            {lowStockCount}
          </div>
          <span style={{ fontSize: '0.75rem', color: '#526b60' }}>Requieren reabastecimiento</span>
        </div>

        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0b5d4b', textTransform: 'uppercase' }}>
            Medicamentos
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0b5d4b', marginTop: '4px' }}>
            {medicamentosCount}
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Fármacos y viales controlados</span>
        </div>
      </div>

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
          {['TODAS', 'Medicamento', 'Insumo', 'Equipo'].map((cat) => (
            <button
              key={cat}
              onClick={() => setFilterCat(cat)}
              style={{
                padding: '6px 12px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: filterCat === cat ? '#0b5d4b' : '#f0f4f2',
                color: filterCat === cat ? '#ffffff' : '#526b60',
              }}
            >
              {cat === 'TODAS' ? 'Todas las Categorías' : cat}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
            <SearchIcon size={16} />
          </div>
          <input
            type="text"
            placeholder="Buscar por nombre, código o ubicación..."
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

      {/* Table */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2ece7',
          overflowX: 'auto',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ background: '#f8faf9', borderBottom: '1px solid #e2ece7', color: '#526b60' }}>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Código / Nombre</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Categoría</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Ubicación</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Stock Actual</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Mínimo</th>
              <th style={{ padding: '14px 16px', fontWeight: 800, textAlign: 'right' }}>
                {isOps ? 'Movimientos de Almacén' : 'Gestión de Catálogo'}
              </th>
            </tr>
          </thead>
          <tbody>
            {filteredItems.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#849a90' }}>
                  {loading ? 'Cargando inventario...' : 'No se encontraron artículos con este criterio.'}
                </td>
              </tr>
            ) : (
              filteredItems.map((item) => {
                const isLow = item.stockActual <= item.stockMinimo;
                return (
                  <tr key={item.id} style={{ borderBottom: '1px solid #f0f4f2' }}>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 800, color: '#13221c' }}>{item.nombre}</div>
                      <div style={{ fontSize: '0.75rem', color: '#0b5d4b', fontWeight: 600 }}>{item.codigo}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '6px',
                          fontSize: '0.72rem',
                          fontWeight: 700,
                          background:
                            item.categoria === 'Medicamento'
                              ? '#ecfdf5'
                              : item.categoria === 'Insumo'
                              ? '#e0f2fe'
                              : '#fef3c7',
                          color:
                            item.categoria === 'Medicamento'
                              ? '#047857'
                              : item.categoria === 'Insumo'
                              ? '#0369a1'
                              : '#b45309',
                        }}
                      >
                        {item.categoria}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#526b60' }}>{item.ubicacion}</td>
                    <td style={{ padding: '14px 16px' }}>
                      <div
                        style={{
                          fontWeight: 800,
                          fontSize: '0.95rem',
                          color: isLow ? '#dc2626' : '#13221c',
                        }}
                      >
                        {item.stockActual} {item.unidad}
                        {isLow && (
                          <span
                            style={{
                              marginLeft: '6px',
                              fontSize: '0.68rem',
                              fontWeight: 800,
                              background: '#fee2e2',
                              color: '#b91c1c',
                              padding: '2px 6px',
                              borderRadius: '4px',
                            }}
                          >
                            BAJO STOCK
                          </span>
                        )}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#849a90' }}>
                      {item.stockMinimo} {item.unidad}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                        {isOps ? (
                          /* JEFE DE OPERACIONES: Movimiento de stock físico */
                          <button
                            onClick={() => {
                              setSelectedItem(item);
                              setMovForm({ tipo: 'ENTRADA', cantidad: 1, motivo: '' });
                              setShowMovModal(true);
                            }}
                            style={{
                              padding: '6px 12px',
                              borderRadius: '6px',
                              border: 'none',
                              background: '#0284c7',
                              color: '#ffffff',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              cursor: 'pointer',
                              boxShadow: '0 2px 6px rgba(2, 132, 199, 0.25)',
                            }}
                          >
                            ± Movimiento
                          </button>
                        ) : (
                          /* ADMINISTRADOR GENERAL: Gestión de catálogo institucional (no mueve stock) */
                          <>
                            <button
                              onClick={() => {
                                setSelectedItem(item);
                                setRecursoForm({
                                  nombre: item.nombre,
                                  categoria: item.categoria,
                                  unidad: item.unidad,
                                  ubicacion: item.ubicacion,
                                  stock_minimo: item.stockMinimo,
                                  cantidad: item.stockActual,
                                });
                                setShowEditModal(true);
                              }}
                              style={{
                                padding: '6px 10px',
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
                              onClick={() => handleDeleteRecurso(item)}
                              style={{
                                padding: '6px 10px',
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
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL REGISTRAR MOVIMIENTO */}
      {showMovModal && selectedItem && (
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
              maxWidth: '440px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Registrar Movimiento de Inventario
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Artículo: <strong>{selectedItem.nombre}</strong> (Stock actual: {selectedItem.stockActual} {selectedItem.unidad})
            </p>

            <form onSubmit={handleRegistrarMovimiento} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Tipo de Movimiento
                </label>
                <select
                  value={movForm.tipo}
                  onChange={(e) => setMovForm({ ...movForm, tipo: e.target.value as any })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                >
                  <option value="ENTRADA">ENTRADA (+ Abastecimiento)</option>
                  <option value="SALIDA">SALIDA (- Despacho a sala o piso)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Cantidad ({selectedItem.unidad})
                </label>
                <input
                  type="number"
                  min={1}
                  value={movForm.cantidad}
                  onChange={(e) => setMovForm({ ...movForm, cantidad: parseInt(e.target.value, 10) || 1 })}
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
                  Motivo o Destino del Movimiento
                </label>
                <textarea
                  value={movForm.motivo}
                  onChange={(e) => setMovForm({ ...movForm, motivo: e.target.value })}
                  placeholder="Ej. Suministro a rampa de emergencias o compra recibida..."
                  rows={3}
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

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowMovModal(false)}
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
                  {isSubmitting ? 'Registrando...' : 'Confirmar Movimiento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL CREAR RECURSO */}
      {showCreateModal && (
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
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Nuevo Recurso de Inventario
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Registra un nuevo medicamento, insumo médico o equipo en la base de datos.
            </p>

            <form onSubmit={handleCreateSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Nombre del Recurso
                </label>
                <input
                  type="text"
                  value={recursoForm.nombre}
                  onChange={(e) => setRecursoForm({ ...recursoForm, nombre: e.target.value })}
                  placeholder="Ej. Ampicilina 1g Polvo Inyectable"
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
                    Categoría
                  </label>
                  <select
                    value={recursoForm.categoria}
                    onChange={(e) => setRecursoForm({ ...recursoForm, categoria: e.target.value as any })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                  >
                    <option value="Medicamento">Medicamento</option>
                    <option value="Insumo">Insumo</option>
                    <option value="Equipo">Equipo</option>
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Unidad de Medida
                  </label>
                  <input
                    type="text"
                    value={recursoForm.unidad}
                    onChange={(e) => setRecursoForm({ ...recursoForm, unidad: e.target.value })}
                    placeholder="Ej. Frasco, Ampolla, Caja"
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
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Ubicación Física
                </label>
                <input
                  type="text"
                  value={recursoForm.ubicacion}
                  onChange={(e) => setRecursoForm({ ...recursoForm, ubicacion: e.target.value })}
                  placeholder="Ej. Farmacia Central / Estante A"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Cantidad Inicial
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={recursoForm.cantidad}
                    onChange={(e) => setRecursoForm({ ...recursoForm, cantidad: parseInt(e.target.value, 10) || 0 })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Stock Mínimo de Alerta
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={recursoForm.stock_minimo}
                    onChange={(e) => setRecursoForm({ ...recursoForm, stock_minimo: parseInt(e.target.value, 10) || 0 })}
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
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
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
                  {isSubmitting ? 'Guardando...' : 'Crear Recurso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL EDITAR RECURSO */}
      {showEditModal && selectedItem && (
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
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Editar Recurso de Inventario
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Nota: El stock físico ({selectedItem.stockActual} {selectedItem.unidad}) solo se modifica mediante la bitácora de movimientos.
            </p>

            <form onSubmit={handleEditSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Nombre del Recurso
                </label>
                <input
                  type="text"
                  value={recursoForm.nombre}
                  onChange={(e) => setRecursoForm({ ...recursoForm, nombre: e.target.value })}
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
                    Categoría
                  </label>
                  <select
                    value={recursoForm.categoria}
                    onChange={(e) => setRecursoForm({ ...recursoForm, categoria: e.target.value as any })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                  >
                    <option value="Medicamento">Medicamento</option>
                    <option value="Insumo">Insumo</option>
                    <option value="Equipo">Equipo</option>
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Unidad de Medida
                  </label>
                  <input
                    type="text"
                    value={recursoForm.unidad}
                    onChange={(e) => setRecursoForm({ ...recursoForm, unidad: e.target.value })}
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
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Ubicación Física
                </label>
                <input
                  type="text"
                  value={recursoForm.ubicacion}
                  onChange={(e) => setRecursoForm({ ...recursoForm, ubicacion: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Stock Mínimo de Alerta
                </label>
                <input
                  type="number"
                  min={0}
                  value={recursoForm.stock_minimo}
                  onChange={(e) => setRecursoForm({ ...recursoForm, stock_minimo: parseInt(e.target.value, 10) || 0 })}
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

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
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
                  {isSubmitting ? 'Guardando...' : 'Guardar Cambios'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
