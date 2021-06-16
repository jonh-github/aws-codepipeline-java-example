package com.github.jonhgithub.aws.codepipeline.java.example.controller;

import com.github.jonhgithub.aws.codepipeline.java.example.CatalogueItemGenerator;
import com.github.jonhgithub.aws.codepipeline.java.example.SpringReactiveRestApplication;
import com.github.jonhgithub.aws.codepipeline.java.example.model.CatalogueItem;
import com.github.jonhgithub.aws.codepipeline.java.example.service.CatalogueCrudService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest(
    classes = SpringReactiveRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CatalogueControllerTest {

  public static final String BASE_PATH = "/api/v1";

  private static WebTestClient client;
  private static CatalogueItem catalogueItem = CatalogueItemGenerator.generateCatalogueItem();

  @LocalServerPort int port;

  @Autowired private CatalogueCrudService catalogueCrudService;

  @Autowired
  public void setApplicationContext(ApplicationContext context) {
    this.client =
        WebTestClient.bindToApplicationContext(context)
            .configureClient()
            .baseUrl(BASE_PATH)
            .build();
  }

  @Test
  @Order(10)
  public void testGetAllCatalogueItems() {

    this.client
        .get()
        .uri(CatalogueController.GET_ITEMS)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.[0].id")
        .isNotEmpty()
        .jsonPath("$.[0].name")
        .isNotEmpty()
        .jsonPath("$.[0].description")
        .isNotEmpty();
  }

  @Test
  @Order(20)
  public void testGetCatalogueItem() throws Exception {

    createCatalogueItem();

    this.client
        .get()
        .uri(replaceId(CatalogueController.GET_ITEM))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.id")
        .isNotEmpty()
        .jsonPath("$.name")
        .isNotEmpty()
        .jsonPath("$.description")
        .isNotEmpty();
  }

  @Test
  @Order(30)
  public void testGetCatalogueItemsStream() throws Exception {

    FluxExchangeResult<CatalogueItem> result =
        this.client
            .get()
            .uri(CatalogueController.GET_ITEMS_STREAM)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(CatalogueItem.class);

    Flux<CatalogueItem> events = result.getResponseBody();
    StepVerifier.create(events)
        .expectNextMatches(catalogueItem -> catalogueItem.getId() == 1l)
        .expectNextMatches(catalogueItem -> catalogueItem.getId() == 2l)
        .expectNextMatches(catalogueItem -> catalogueItem.getId() == 3l)
        .thenCancel()
        .verify();
  }

  @Test
  @Order(40)
  public void testCreateCatalogueItem() {
    CatalogueItem item = CatalogueItemGenerator.generateCatalogueItem();
    item.setId(null);

    this.client
        .post()
        .uri(CatalogueController.CREATE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(item), CatalogueItem.class)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON);
  }

  @Test
  @Order(40)
  public void testUpdateCatalogueItem() throws Exception {
    createCatalogueItem();

    this.client
        .put()
        .uri(replaceId(CatalogueController.UPDATE))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(CatalogueItemGenerator.generateCatalogueItem()), CatalogueItem.class)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @Order(50)
  public void testDeleteCatalogueItem() throws Exception {
    createCatalogueItem();

    this.client
        .delete()
        .uri(replaceId(CatalogueController.DELETE))
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  /** Test method to validate create catalogue item if Invalid Category is passed in request */
  @Test
  @Order(60)
  public void testCreateCatalogueItemWithInvalidCategory() {

    CatalogueItem catalogueItem = CatalogueItemGenerator.generateCatalogueItem();
    catalogueItem.setCategory("INVALID");

    this.client
        .post()
        .uri(CatalogueController.CREATE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(catalogueItem), CatalogueItem.class)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON);
  }

  /** Test method to validate Resource not found exception */
  @Test
  @Order(70)
  public void testResourceNotFoundException() throws Exception {

    this.client
        .get()
        .uri(CatalogueController.GET_ITEM.replaceAll("\\{id\\}", "-1"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON);
  }

  private void createCatalogueItem() {
    CatalogueItem item = CatalogueItemGenerator.generateCatalogueItem();
    item.setId(null);

    this.client
        .post()
        .uri(CatalogueController.CREATE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(item), CatalogueItem.class)
        .exchange()
        .expectStatus()
        .isCreated();
  }

  private String replaceId(String path) {
    return path.replaceAll("\\{id\\}", catalogueItem.getId().toString());
  }
}
