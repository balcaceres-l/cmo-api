"""Genera cmo-plan.html incrustando backlog.json en plan.template.html.

Uso (desde cualquier carpeta):  python docs/plan/build.py
"""
import json
from pathlib import Path

aqui = Path(__file__).parent
datos = json.loads((aqui / "backlog.json").read_text(encoding="utf-8"))
# "</" dentro de un <script> cerraría la etiqueta antes de tiempo.
incrustado = json.dumps(datos, ensure_ascii=False).replace("</", "<\\/")
plantilla = (aqui / "plan.template.html").read_text(encoding="utf-8")
(aqui / "cmo-plan.html").write_text(plantilla.replace("__BACKLOG__", incrustado), encoding="utf-8")
print("cmo-plan.html generado:", len(datos["hu"]), "HU")
