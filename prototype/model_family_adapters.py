# 20261009 adapted from model/notebooks/LangCalc-evaluation.ipynb
# 20261014 add attn_implementation="eager" for FunctionGemma

# The two model families we support use different chat templates and different tags for tool calls:
# "functiongemma": FunctionGemma-style models (e.g. the fine-tuned jedick/functiongemma-langcalc-en.zh).
#   Uses a "developer" role message, <start_function_call>/<end_function_call> tags,
#   and <escape> for quoting string arguments. 
# "gemma4": Gemma 4 models (e.g. google/gemma-4-E2B).
#   Uses a "system" role message, <|tool_call>/<tool_call|> tags,
#   and <|"|> for quoting string arguments. Attaches "tool_calls" and "tool_responses"
#   to a single assistant message instead of a separate "tool" role message.

# detect_model_family() guesses the family from the model id:
# anything with "gemma-4" in the id is treated as "gemma4",
# everything else as "functiongemma".

import re


def detect_model_family(model_id):
    return "gemma4" if "gemma-4" in model_id.lower() else "functiongemma"


def cast_value(v):
    try:
        return int(v)
    except ValueError:
        try:
            return float(v)
        except ValueError:
            return {"true": True, "false": False}.get(v.lower(), v.strip("'\""))


def extract_tool_calls_functiongemma(text):
    return [
        {
            "name": name,
            "arguments": {
                k: cast_value((v1 or v2).strip())
                for k, v1, v2 in re.findall(r"(\w+):(?:<escape>(.*?)<escape>|([^,}]*))", args)
            },
        }
        for name, args in re.findall(
            r"<start_function_call>call:(\w+)\{(.*?)\}<end_function_call>", text, re.DOTALL
        )
    ]


def extract_tool_calls_gemma4(text):
    return [
        {
            "name": name,
            "arguments": {
                k: cast_value((v1 or v2).strip())
                for k, v1, v2 in re.findall(r'(\w+):(?:<\|"\|>(.*?)<\|"\|>|([^,}]*))', args)
            },
        }
        for name, args in re.findall(
            r"<\|tool_call>call:(\w+)\{(.*?)\}<tool_call\|>", text, re.DOTALL
        )
    ]


FAMILY_CONFIGS = {
    "functiongemma": {
        "system_role": "developer",
        "system_message": "You are a model that can do function calling with the following functions",
        "extract_tool_calls": extract_tool_calls_functiongemma,
        "generate_kwargs": lambda processor: {"pad_token_id": processor.eos_token_id},
        # FunctionGemma fine-tuning examples use eager attention; sdpa gives garbage first tokens for some batch shapes in evaluation
        "model_kwargs": {"attn_implementation": "eager"},
    },
    "gemma4": {
        "system_role": "system",
        "system_message": "You are a model that can do function calling for a calculator app.",
        "extract_tool_calls": extract_tool_calls_gemma4,
        "generate_kwargs": lambda processor: {},
        "model_kwargs": {},
    },
}

