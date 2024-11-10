import subprocess
import os
from datetime import datetime

# List of targets
targets = [
    #"http://127.0.0.1:8080/tukano-base/rest",
    #"https://tukano-scc-baseline.azurewebsites.net/rest",
    #"https://tukano-scc-nocache-cosmos.azurewebsites.net/rest",
    #"https://tukano-scc-nocache-postgres.azurewebsites.net/rest",
    #"https://tukano-scc-final-cosmos.azurewebsites.net/rest",
    "https://tukano-scc-final-postgres.azurewebsites.net/rest"
]

target_description = [
    #"Baseline",
    #"Azure_Baseline",
    #"Azure_NoCache_Cosmos",
    #"Azure_NoCache_Postgres",
    #"Azure_Final_Cosmos",
    "Azure_Final_Postgres"
]

# List of YAML files to execute for each target
yaml_files = [
    "user_register.yaml",
    "upload_shorts.yaml",
    "realistic_flow.yaml",
    "user_delete.yaml"
]

# Number of times to repeat the test for each target
repeat_count = 1  # Set the desired number of repetitions

# Directory to save logs
log_dir = "benchmark_logs"
os.makedirs(log_dir, exist_ok=True)

def run_artillery(target, yaml_file, log_file):
    """
    Runs an Artillery test with a specified target and YAML file, 
    and saves the output to a log file.
    """
    # Set the target environment variable
    os.environ["TARGET_URL"] = target
    
    # Run the artillery command
    result = subprocess.run(["artillery", "run", yaml_file], shell=True, capture_output=True, text=True)
    
    # Write output to log file
    with open(log_file, "a") as f:
        f.write(f"Running {yaml_file} on {target}\n")
        f.write(result.stdout)
        if result.returncode != 0:
            f.write(f"Error: {result.stderr}\n")
        f.write("\n" + "="*50 + "\n\n")

    # Reset the environment variable
    del os.environ["TARGET_URL"]

def main():
    target_count = 0
    for target in targets:
        for i in range(repeat_count):
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            log_file = os.path.join(log_dir, f"benchmark_log_targetdesc_{target_description[target_count]}_test_{i+1}_{timestamp}.txt")
            for yaml_file in yaml_files:
                print(f"Running {yaml_file} on {target}")
                run_artillery(target, yaml_file, log_file)
                print(f"Completed {yaml_file} on {target}")
        target_count += 1
    
    print(f"All benchmarks completed.")

if __name__ == "__main__":
    main()