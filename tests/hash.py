from start_lib import *
from base64 import b64decode


class Config(dict):
  @property
  def algs(self) -> set[str]:
    return set(self["algs"])

  @property
  def path(self) -> str:
    return self["path"]

  @property
  def path_obj(self):
    return Path(self.path)


@ms.utils.main_func(__name__)
def main():
  data = Config(JSON_FILE.read_json())
  ext = MS2Ext()
  p, stderr = ext.run("file_hasher_v1", data, stderr=subprocess.PIPE)
  result: dict[str, str] = ms.json.decode(stderr)
  py_hashes = data.path_obj.ms_path.multi_hash(data.algs)
  for k, v in result.items():
    vb = b64decode(v)
    if py_hashes[k] == vb:
      log("%s: Ok (%s)", k, py_hashes[k].hex())
    else:
      log("%s: Error (%s != %s)", k, py_hashes[k].hex(), vb.hex())
