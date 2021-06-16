package com.github.jonhgithub.aws.codepipeline.java.example.service;

import com.github.jonhgithub.aws.codepipeline.java.example.exception.ResourceNotFoundException;
import com.github.jonhgithub.aws.codepipeline.java.example.model.CatalogueItem;
import com.github.jonhgithub.aws.codepipeline.java.example.repository.CatalogueRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service class to handle Catalogue Item CRUD Operations.
 *
 * @author Jon Harvey
 */
@Slf4j
@Service
public class CatalogueCrudService {

  private final CatalogueRepository catalogueRepository;

  CatalogueCrudService(CatalogueRepository catalogueRepository) {
    this.catalogueRepository = catalogueRepository;
  }

  public Flux<CatalogueItem> getCatalogueItems() {
    Sort sort = Sort.by(Sort.Direction.ASC, "name");

    return catalogueRepository.findAll(sort);
  }

  public Mono<CatalogueItem> getCatalogueItem(Long id) throws ResourceNotFoundException {
    return getCatalogueItemById(id);
  }

  public Mono<Long> addCatalogItem(CatalogueItem catalogueItem) {
    catalogueItem.setCreatedOn(Instant.now());

    return catalogueRepository.save(catalogueItem).flatMap(item -> Mono.just(item.getId()));
  }

  public void updateCatalogueItem(Long id, CatalogueItem catalogueItem)
      throws ResourceNotFoundException {

    Mono<CatalogueItem> catalogueItemfromDB = getCatalogueItemById(id);

    catalogueItemfromDB.subscribe(
        value -> {
          value.setName(catalogueItem.getName());
          value.setDescription(catalogueItem.getDescription());
          value.setPrice(catalogueItem.getPrice());
          value.setInventory(catalogueItem.getInventory());
          value.setUpdatedOn(Instant.now());

          catalogueRepository.save(value).subscribe();
        });
  }

  public void deleteCatalogueItem(CatalogueItem catalogueItem) {

    // For delete to work as expected, we need to subscribe() for the flow to complete
    catalogueRepository.delete(catalogueItem).subscribe();
  }

  private Mono<CatalogueItem> getCatalogueItemById(Long id) throws ResourceNotFoundException {
    return catalogueRepository
        .findById(id)
        .switchIfEmpty(
            Mono.defer(
                () ->
                    Mono.error(
                        new ResourceNotFoundException(
                            String.format(
                                "Catalogue Item not found for the provided id :: %s", id)))));
  }
}
