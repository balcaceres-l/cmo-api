import React from 'react';
import { BedData, MedicoOption, ModalType, PacienteOption } from '../types';

interface BedModalsProps {
  modalType: ModalType;
  selectedBed: BedData | null;
  pacientes: PacienteOption[];
  medicos: MedicoOption[];
  availableBedsForTransfer: BedData[];
  ingresoForm: {
    id_paciente: string;
    tipo: 'VIP' | 'SEMIPRIVADA';
    id_medico: string;
    motivo: string;
  };
  trasladoIdCama: string;
  isSubmitting: boolean;
  onChangeIngresoForm: (next: {
    id_paciente: string;
    tipo: 'VIP' | 'SEMIPRIVADA';
    id_medico: string;
    motivo: string;
  }) => void;
  onChangeTrasladoIdCama: (id: string) => void;
  onClose: () => void;
  onSubmitIngreso: (e: React.FormEvent) => void;
  onConfirmAlta: () => void;
  onSubmitTraslado: (e: React.FormEvent) => void;
}

export const BedModals: React.FC<BedModalsProps> = ({
  modalType,
  selectedBed,
  pacientes,
  medicos,
  availableBedsForTransfer,
  ingresoForm,
  trasladoIdCama,
  isSubmitting,
  onChangeIngresoForm,
  onChangeTrasladoIdCama,
  onClose,
  onSubmitIngreso,
  onConfirmAlta,
  onSubmitTraslado,
}) => {
  if (!modalType) return null;

  const overlayStyle: React.CSSProperties = {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 55,
    padding: '16px',
  };

  const modalCardStyle: React.CSSProperties = {
    background: '#ffffff',
    borderRadius: '16px',
    maxWidth: '480px',
    width: '100%',
    padding: '24px',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
  };

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '8px 12px',
    borderRadius: '8px',
    border: '1px solid #cad8d1',
    fontSize: '0.85rem',
  };

  if (modalType === 'INGRESO') {
    return (
      <div style={overlayStyle}>
        <div style={modalCardStyle}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
            Asignar Ingreso Hospitalario
          </h3>
          <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
            {selectedBed
              ? `Asignación directa para la cama ${selectedBed.codigo} (${selectedBed.tipo} - Planta ${selectedBed.planta}).`
              : 'Asignación hospitalaria según capacidad disponible.'}
          </p>

          <form onSubmit={onSubmitIngreso} style={{ display: 'grid', gap: '14px' }}>
            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                Paciente
              </label>
              <select
                value={ingresoForm.id_paciente}
                onChange={(e) => onChangeIngresoForm({ ...ingresoForm, id_paciente: e.target.value })}
                style={inputStyle}
                required
              >
                <option value="">-- Seleccionar Paciente --</option>
                {pacientes.map((p) => (
                  <option key={p.id_paciente} value={p.id_paciente}>
                    {p.nombre_completo} ({p.numero_expediente})
                  </option>
                ))}
              </select>
            </div>

            {!selectedBed && (
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Tipo de Habitación Requerida
                </label>
                <select
                  value={ingresoForm.tipo}
                  onChange={(e) =>
                    onChangeIngresoForm({ ...ingresoForm, tipo: e.target.value as 'VIP' | 'SEMIPRIVADA' })
                  }
                  style={inputStyle}
                >
                  <option value="SEMIPRIVADA">Semiprivada (Planta 2 - 12 Camas)</option>
                  <option value="VIP">VIP Individual (Planta 3 - 5 Camas)</option>
                </select>
              </div>
            )}

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                Médico Tratante a Cargo
              </label>
              <select
                value={ingresoForm.id_medico}
                onChange={(e) => onChangeIngresoForm({ ...ingresoForm, id_medico: e.target.value })}
                style={inputStyle}
                required
              >
                <option value="">-- Seleccionar Médico --</option>
                {medicos.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.nombre_completo} ({m.especialidad_nombre || m.cargo || 'Médico'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                Motivo o Diagnóstico de Ingreso
              </label>
              <textarea
                value={ingresoForm.motivo}
                onChange={(e) => onChangeIngresoForm({ ...ingresoForm, motivo: e.target.value })}
                placeholder="Ej. Postoperatorio inmediato de colecistectomía..."
                rows={3}
                style={inputStyle}
                required
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
              <button
                type="button"
                onClick={onClose}
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
                {isSubmitting ? 'Asignando...' : 'Confirmar Ingreso'}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  if (modalType === 'ALTA' && selectedBed) {
    return (
      <div style={overlayStyle}>
        <div style={{ ...modalCardStyle, maxWidth: '440px' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
            Confirmar Alta Hospitalaria
          </h3>
          <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#526b60' }}>
            ¿Confirmas el alta médica de <strong>{selectedBed.paciente?.nombre}</strong>? La cama{' '}
            <strong>{selectedBed.codigo}</strong> quedará liberada y disponible inmediatamente.
          </p>

          <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              onClick={onClose}
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
              Volver
            </button>
            <button
              type="button"
              onClick={onConfirmAlta}
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
              {isSubmitting ? 'Procesando...' : 'Confirmar Alta'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (modalType === 'TRASLADO' && selectedBed) {
    return (
      <div style={overlayStyle}>
        <div style={{ ...modalCardStyle, maxWidth: '460px' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
            Trasladar Paciente de Cama
          </h3>
          <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
            Paciente: <strong>{selectedBed.paciente?.nombre}</strong> (Origen: {selectedBed.codigo}). Se conserva la
            fecha de ingreso original sin generar alta ficticia.
          </p>

          <form onSubmit={onSubmitTraslado} style={{ display: 'grid', gap: '14px' }}>
            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                Cama de Destino Disponible
              </label>
              <select
                value={trasladoIdCama}
                onChange={(e) => onChangeTrasladoIdCama(e.target.value)}
                style={inputStyle}
                required
              >
                <option value="">-- Seleccionar Cama Disponible --</option>
                {availableBedsForTransfer.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.codigo} ({b.tipo} - Planta {b.planta})
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
              <button
                type="button"
                onClick={onClose}
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
                disabled={isSubmitting || !trasladoIdCama}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#0369a1',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {isSubmitting ? 'Trasladando...' : 'Ejecutar Traslado'}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  return null;
};
