import os

BASE = os.path.dirname(os.path.realpath(__file__))
TOP = os.path.realpath(os.path.join(BASE, ".."))
DATA = os.path.join(BASE, "data")

# OS paths
INSTALL_SH_PATH = os.path.join(os.getcwd(), "install_packages.sh")

# Web paths
SF_URL = "https://square.snowflakecomputing.com/"