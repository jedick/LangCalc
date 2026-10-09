"""Build a Hugging Face dataset from the LangCalc train/test CSVs and push it to the Hub.

Each split combines one CSV file per decision ("calculate" and "decline"). The script inserts
a "decision" column that records which file each row came from.
"""

from collections import Counter
from pathlib import Path

import pandas as pd
from datasets import Dataset, DatasetDict
from huggingface_hub import login, whoami

# Set the language here
lang = "en"

# Directory that holds the CSV files
data_dir = Path(".")

# One CSV file per decision and split: {split}_{decision}_{lang}.csv
decisions = ["calculate", "decline"]
splits = ["train", "test"]

# Columns expected in each CSV file (the decision column is inserted by this script)
csv_columns = ["origin", "scenario", "prompt", "function", "x", "y", "reason"]

# Columns of the pushed dataset, in order
columns = ["origin", "scenario", "prompt", "decision", "function", "x", "y", "reason"]

# The functions that each decision may use
functions = {
    "calculate": ["add", "subtract", "multiply", "divide"],
    "decline": ["decline"],
}


def read_split(split):
    """Read the CSV file for each decision, add the decision column, and stack the rows"""
    frames = []
    for decision in decisions:
        csv_path = data_dir / f"{split}_{decision}_{lang}.csv"
        # Read text columns as strings and treat only empty fields as missing, so that
        # a prompt like "88" or "None" is not converted to a number or NaN
        df = pd.read_csv(
            csv_path,
            dtype={
                "origin": str,
                "scenario": str,
                "prompt": str,
                "function": str,
                "reason": str,
            },
            keep_default_na=False,
            na_values=[""],
        )

        # Check the column names
        missing = [c for c in csv_columns if c not in df.columns]
        extra = [c for c in df.columns if c not in csv_columns]
        if missing or extra:
            raise ValueError(f"{csv_path}: missing columns {missing}, unexpected columns {extra}")

        # Check that each function belongs to this file's decision
        bad = ~df["function"].isin(functions[decision])
        if bad.any():
            # Report CSV line numbers (the header is line 1)
            lines = (df.index[bad] + 2).tolist()
            found = sorted(df.loc[bad, "function"].fillna("<empty>").unique())
            raise ValueError(
                f"{csv_path}: decision '{decision}' allows functions {functions[decision]} "
                f"but found {found} on lines {lines}"
            )

        df["decision"] = decision
        print(f"{csv_path}: {len(df)} rows")
        frames.append(df)

    df = pd.concat(frames, ignore_index=True)
    return Dataset.from_pandas(df[columns], preserve_index=False)


dataset = DatasetDict({split: read_split(split) for split in splits})

for split, ds in dataset.items():
    print(f"{split} examples: {len(ds)} {dict(Counter(ds['decision']))}")
print(dataset["train"].features)

# Uses HF_TOKEN from the environment if set, otherwise prompts for a token.
login()

# Construct the HF repo_id for the dataset
username = whoami()["name"]
repo_id = f"{username}/langcalc-{lang}"

dataset.push_to_hub(repo_id, private=False)
print(f"Pushed dataset to https://huggingface.co/datasets/{repo_id}")
