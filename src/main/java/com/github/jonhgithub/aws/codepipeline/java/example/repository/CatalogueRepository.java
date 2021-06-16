package com.github.jonhgithub.aws.codepipeline.java.example.repository;

import com.github.jonhgithub.aws.codepipeline.java.example.model.CatalogueItem;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

public interface CatalogueRepository extends ReactiveSortingRepository<CatalogueItem, Long> {}
