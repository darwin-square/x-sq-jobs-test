# Databricks notebook source
# GLOBAL IMPORTS

import os
import sys
import subprocess
import tasks.locations as loc

def install_dependencies() -> None:
    """Runs the install_packages.sh file for any project dependencies."""
    subprocess.run(["bash", loc.INSTALL_SH_PATH])

install_dependencies()

# COMMAND ----------

# Hacky way to initialize python project in Databricks.
if os.getcwd().rsplit("/", 1)[0] not in sys.path:
    sys.path.append(os.getcwd().rsplit("/", 1)[0])