import os
import sys

def check_repo():
    required_files = ["Jenkinsfile"]
    missing = [f for f in required_files if not os.path.exists(f)]

    print("=== Repo Structure Check ===")
    for f in os.listdir("."):
        print(f"  - {f}")

    if missing:
        print(f"MISSING required files: {missing}")
        sys.exit(1)
    else:
        print("All required files present.")
        sys.exit(0)

if __name__ == "__main__":
    check_repo()