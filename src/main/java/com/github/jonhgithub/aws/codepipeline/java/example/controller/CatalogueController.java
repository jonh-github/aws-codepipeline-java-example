package com.github.jonhgithub.aws.codepipeline.java.example.controller;

import com.github.jonhgithub.aws.codepipeline.java.example.exception.ResourceNotFoundException;
import com.github.jonhgithub.aws.codepipeline.java.example.model.CatalogueItem;
import com.github.jonhgithub.aws.codepipeline.java.example.model.ResourceIdentity;
import com.github.jonhgithub.aws.codepipeline.java.example.service.CatalogueCrudService;
import java.time.Duration;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller class to handle Catalogue Item CRUD operations.
 *
 * @author Jon Harvey
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class CatalogueController {

  static final String CREATE = "/";
  static final String GET_ITEMS = "/";
  static final String GET_ITEMS_STREAM = "/stream";
  static final String GET_ITEM = "/{id}";
  static final String UPDATE = "/{id}";
  static final String DELETE = "/{id}";

  private final CatalogueCrudService catalogueCrudService;

  public CatalogueController(CatalogueCrudService catalogueCrudService) {
    this.catalogueCrudService = catalogueCrudService;
  }

  /**
   * Get Catalogue Items available in database
   *
   * @return catalogueItems
   */
  @GetMapping(GET_ITEMS)
  @ResponseStatus(value = HttpStatus.OK)
  public Flux<CatalogueItem> getCatalogueItems() {
    return catalogueCrudService.getCatalogueItems();
  }

  /**
   * If api needs to push items as Streams to ensure Backpressure is applied, we need to set
   * produces to MediaType.TEXT_EVENT_STREAM_VALUE
   *
   * <p>MediaType.TEXT_EVENT_STREAM_VALUE is the official media type for Server Sent Events (SSE)
   * MediaType.APPLICATION_STREAM_JSON_VALUE is for server to server/http client communications.
   *
   * <p>https://stackoverflow.com/questions/52098863/whats-the-difference-between-text-event-stream-and-application-streamjson
   *
   * @return catalogueItems
   */
  @GetMapping(path = GET_ITEMS_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @ResponseStatus(value = HttpStatus.OK)
  public Flux<CatalogueItem> getCatalogueItemsStream() {
    return catalogueCrudService.getCatalogueItems().delayElements(Duration.ofMillis(200));
  }

  /**
   * Get Catalogue Item
   *
   * @param id
   * @return catalogueItem
   * @throws ResourceNotFoundException
   */
  @GetMapping(GET_ITEM)
  public Mono<CatalogueItem> getCatalogueItem(@PathVariable(value = "id") Long id)
      throws ResourceNotFoundException {

    return catalogueCrudService.getCatalogueItem(id);
  }

  /**
   * Create Catalogue Item
   *
   * @param catalogueItem
   * @return id of created CatalogueItem
   */
  @PostMapping(CREATE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public Mono<ResponseEntity> addCatalogueItem(@Valid @RequestBody CatalogueItem catalogueItem) {

    Mono<Long> id = catalogueCrudService.addCatalogItem(catalogueItem);

    return id.map(
            value -> ResponseEntity.status(HttpStatus.CREATED).body(new ResourceIdentity(value)))
        .cast(ResponseEntity.class);
  }

  /**
   * Update Catalogue Item
   *
   * @param skuNumber
   * @param catalogueItem
   * @throws ResourceNotFoundException
   */
  @PutMapping(UPDATE)
  @ResponseStatus(value = HttpStatus.OK)
  public void updateCatalogueItem(
      @PathVariable(value = "id") Long id, @Valid @RequestBody CatalogueItem catalogueItem)
      throws ResourceNotFoundException {

    catalogueCrudService.updateCatalogueItem(id, catalogueItem);
  }

  /**
   * Delete Catalogue Item
   *
   * @param id
   * @throws ResourceNotFoundException
   */
  @DeleteMapping(DELETE)
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void removeCatalogItem(@PathVariable(value = "id") Long id)
      throws ResourceNotFoundException {

    Mono<CatalogueItem> catalogueItem = catalogueCrudService.getCatalogueItem(id);
    catalogueItem.subscribe(
        value -> {
          catalogueCrudService.deleteCatalogueItem(value);
        });
  }
}
