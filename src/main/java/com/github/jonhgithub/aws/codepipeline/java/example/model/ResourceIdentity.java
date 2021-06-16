package com.github.jonhgithub.aws.codepipeline.java.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceIdentity {

  @NonNull private Long id;
}
