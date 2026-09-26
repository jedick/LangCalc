package com.jedick.langcalc.data

/**
 * Identifies the single fine-tuned FunctionGemma checkpoint this app uses, as published on the
 * Hugging Face Hub. Kept in one place because the download URL, the update check, and the local
 * file name all need to agree.
 *
 * See model_allowlist_test.json in the main LangCalc repo's gallery-task/ folder, which is where
 * these values were sourced from -- this app replaces that allowlist-driven flow with a direct
 * download, since there's no Gallery model manager here and Gemma 4 has been dropped entirely.
 */
object ModelConstants {
  const val REPO_ID = "jedick/functiongemma-langcalc-en.zh"
  const val FILENAME = "langcalc_q8_ekv1024.litertlm"
  const val APPROX_SIZE_BYTES = 285_161_856L
  const val MIN_DEVICE_MEMORY_GB = 6

  /** Hugging Face's standard "resolve" URL for a file on the main branch of a public repo. */
  fun downloadUrl(): String = "https://huggingface.co/$REPO_ID/resolve/main/$FILENAME"

  /** HF's API for a single repo file's metadata (used for the update check). */
  fun metadataUrl(): String = "https://huggingface.co/api/models/$REPO_ID"
}
