import subprocess
from functools import cached_property
from MainShortcuts2 import ms
from MainShortcuts2.ex.pathlib_ex import Path
from zipfile import ZipFile
ROOT_DIR = Path(ms.MAIN_DIR).parent
BUILD_DIR = ROOT_DIR / "build/libs"
JSON_FILE = Path(ms.MAIN_FILE).with_suffix(".json")
log = ms.utils.mini_log


class InfoDict(dict):
  @property
  def classes(self) -> dict[str, str]:
    return self["classes"]

  @property
  def version_id(self) -> int:
    return self["version_id"]


class MS2Ext:
  @cached_property
  def all_jars(self):
    result: dict[Path, InfoDict] = {}
    for file in BUILD_DIR.iterdir():
      if (file.suffix == ".jar") and file.is_file():
        with file.open("rb") as f:
          zip = ZipFile(f, "r")
          json = zip.read("info.json").decode("utf-8")
        data = ms.json.decode(json)
        result[file] = InfoDict(data)
    return result

  @cached_property
  def info(self):
    return self.all_jars[self.jar]

  @cached_property
  def jar(self):
    result = max(self.all_jars, key=lambda i: self.all_jars[i].version_id)
    log("Selected JAR: %s", result.name)
    return result

  def run(self, name: str, data: dict, **kw):
    kw["args"] = ["java", "-cp", str(self.jar), self.info.classes[name]]
    kw["stdin"] = subprocess.PIPE
    with subprocess.Popen(**kw) as p:
      p.stdin.write(ms.json.encode(data).encode("utf-8"))
      p.stdin.flush()
      p.stdin.close()
      if p.wait():
        raise subprocess.CalledProcessError(p.returncode, kw["args"], p.stdout, p.stderr)
      return p
