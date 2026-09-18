"""Build a Hugging Face dataset from the LangCalc train/test CSVs and push it to the Hub.
"""

from datasets import Dataset, DatasetDict
from huggingface_hub import login, whoami

# Build the dataset from these CSV files
train_csv_path = "langcalc_train_data.csv"
test_csv_path = "langcalc_test_data.csv"

dataset = DatasetDict(
    {
        "train": Dataset.from_csv(train_csv_path),
        "test": Dataset.from_csv(test_csv_path),
    }
)

print(f"Train examples: {len(dataset['train'])}")
print(f"Test examples: {len(dataset['test'])}")

# Uses HF_TOKEN from the environment if set, otherwise prompts for a token.
login()

# Construct the HF repo_id for the dataset
username = whoami()["name"]
repo_id = f"{username}/langcalc-data"

dataset.push_to_hub(repo_id)
print(f"Pushed dataset to https://huggingface.co/datasets/{repo_id}")
