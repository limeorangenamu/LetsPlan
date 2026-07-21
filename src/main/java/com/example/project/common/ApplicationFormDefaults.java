package com.example.project.common;

public class ApplicationFormDefaults {
  public static final String DEFAULT_FORM_SCHEMA_JSON =
  "[{\"question\":\"가입 동기가 무엇인가요?\"}]";

  public static boolean isEmptySchema(String formSchema){
    return formSchema == null || formSchema.isBlank() || "[]".equals(formSchema.trim());
  }
}