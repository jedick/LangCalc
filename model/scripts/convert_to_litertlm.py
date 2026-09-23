# Conversion to .litertlm for on-device deployment

# 20260815 jmd first version:
#   - install litert-torch instead of ai-edge-litert-nightly
#   - use hf_tokenizer_model_path instead of tokenizer_model_path
# 20260923
#  - load checkpoint from HF Hub repo and upload .litertlm back to it
#  - move start_token_id and stop_tokens out of llm_metadata
#  - add litert-lm tag to model card

# Modified from Finetune_FunctionGemma_270M_for_Mobile_Actions_with_Hugging_Face.ipynb
# https://github.com/google-gemini/gemma-cookbook/blob/main/FunctionGemma/%5BFunctionGemma%5DFinetune_FunctionGemma_270M_for_Mobile_Actions_with_Hugging_Face.ipynb

#!pip install litert-torch huggingface_hub ruamel.yaml

# Build the .litertlm from the fine-tuned model hosted on the Hugging Face Hub,
# then upload the converted .litertlm file back to the same repo so it can be
# fetched by the AI Edge Gallery custom task over the internet. Requires prior
# `huggingface-cli login` (or HF_TOKEN set) with write access to the repo.

import io
import os
import re
import tempfile
from huggingface_hub import snapshot_download, hf_hub_download, HfApi
from litert_torch.generative.examples.gemma3 import gemma3
from litert_torch.generative.utilities import converter
from litert_torch.generative.utilities.export_config import ExportConfig
from litert_torch.generative.layers import kv_cache
from ruamel.yaml import YAML

# Hugging Face repo holding the fine-tuned checkpoint (input) and where the
# converted .litertlm file will be uploaded (output)
hf_repo_id = "jedick/functiongemma-langcalc-en.zh"


def add_readme_tag(repo_id, tag):
    """Add `tag` to the tags list in the model card's YAML frontmatter,
    leaving every other line of README.md untouched. No-ops if the tag is
    already present; adds a new tags list only if the README has none."""
    readme_path = hf_hub_download(repo_id=repo_id, filename="README.md")
    with open(readme_path, "r", encoding="utf-8") as f:
        text = f.read()

    match = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, flags=re.DOTALL)
    if not match:
        print("README.md has no YAML frontmatter; skipping tag update.")
        return
    frontmatter_text, body = match.group(1), match.group(2)

    yaml = YAML()
    yaml.preserve_quotes = True
    data = yaml.load(frontmatter_text)

    tags = data.get("tags")
    if tags is None:
        data["tags"] = [tag]
    elif tag not in tags:
        tags.append(tag)
    else:
        print(f"'{tag}' tag already present in README.md; nothing to update.")
        return

    buf = io.StringIO()
    yaml.dump(data, buf)
    new_frontmatter = buf.getvalue().rstrip("\n")
    new_text = f"---\n{new_frontmatter}\n---\n{body}"

    with tempfile.TemporaryDirectory() as tmp_dir:
        updated_path = os.path.join(tmp_dir, "README.md")
        with open(updated_path, "w", encoding="utf-8") as f:
            f.write(new_text)
        HfApi().upload_file(
            path_or_fileobj=updated_path,
            path_in_repo="README.md",
            repo_id=repo_id,
            repo_type="model",
        )

    print(f"Added '{tag}' tag to README.md on https://huggingface.co/{repo_id}")


# Metadata for FunctionGemma. The model type has to go through this proto route:
# build_litertlm's llm_model_type string kwarg only accepts
# generic/gemma3n/gemma3/qwen3/qwen2p5, not function_gemma,
# even though function_gemma is a valid field in the proto.
llm_metadata = r"""llm_model_type: {
    function_gemma: {}
}
"""

# Use a temporary directory for the converted output;
# it's discarded once the upload to the Hub completes
with tempfile.TemporaryDirectory() as litertlm_output_dir:

    # Create the LLM metadata file
    metadata_path = os.path.join(litertlm_output_dir, 'base_llm_metadata.textproto')
    with open(metadata_path, 'w') as f:
        f.write(llm_metadata)

    # Download the fine-tuned checkpoint from the Hugging Face repo, using the
    # default cache and skipping the .litertlm file(s) already in the repo
    # (that's this script's output, not an input needed for conversion)
    checkpoint_dir = snapshot_download(
        repo_id=hf_repo_id,
        ignore_patterns=["*.litertlm"],
    )

    # Import the weights and build the PyTorch model
    pytorch_model = gemma3.build_model_270m(checkpoint_dir)

    # Setup the export configurations and parameters for text generation models.
    export_config = ExportConfig()
    export_config.kvcache_layout = kv_cache.KV_LAYOUT_TRANSPOSED
    export_config.mask_as_input = True

    # This is only for SentencePiece
    #tokenizer_model_path = os.path.join(checkpoint_dir, 'tokenizer.model')

    # For models fine-tuned with HF transformers
    hf_tokenizer_model_path = os.path.join(checkpoint_dir, 'tokenizer.json')

    # Convert to LiteRT-LM Format
    converter.convert_to_litert(
        # Arguments for convert_to_litert
        pytorch_model,
        output_path=litertlm_output_dir,
        output_name_prefix="langcalc",
        prefill_seq_len=256,
        kv_cache_max_len=1024,
        quantize="dynamic_int8",
        export_config=export_config,
        output_format="litertlm",
        # Remaining kwargs passed to build_litertlm
        #tokenizer_model_path=tokenizer_model_path,
        hf_tokenizer_model_path=hf_tokenizer_model_path,
        base_llm_metadata_path=metadata_path,
        start_token_id=2,
        stop_tokens=["<end_of_turn>", "<start_function_response>"],
    )

    # Identify the converted .litertlm file, e.g. langcalc_q8_ekv1024.litertlm
    litertlm_files = [f for f in os.listdir(litertlm_output_dir) if f.endswith('.litertlm')]
    if len(litertlm_files) != 1:
        raise RuntimeError(
            f"Expected exactly one .litertlm file in {litertlm_output_dir}, found: {litertlm_files}"
        )
    litertlm_filename = litertlm_files[0]
    litertlm_path = os.path.join(litertlm_output_dir, litertlm_filename)

    # Upload the .litertlm file to the same Hugging Face repo, at the repo root
    # (matching the layout of litert-community/functiongemma-270m-ft-mobile-actions,
    # so the AI Edge Gallery custom task can reference it by filename alone)
    api = HfApi()
    api.upload_file(
        path_or_fileobj=litertlm_path,
        path_in_repo=litertlm_filename,
        repo_id=hf_repo_id,
        repo_type="model",
    )

    print(f"Uploaded {litertlm_filename} to https://huggingface.co/{hf_repo_id}")

# Tag the model card so it's discoverable as a litert-lm model
add_readme_tag(hf_repo_id, "litert-lm")
