from start_lib import *


@ms.utils.main_func(__name__)
def main():
  data = JSON_FILE.read_json()
  ext = MS2Ext()
  ext.run("file_downloader_v1", data)
