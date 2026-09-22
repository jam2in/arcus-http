package com.jam2in.arcus.http.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

class LayerBoundaryTest {

  @Test
  @DisplayName("모든 Controller는 Service에 의존하며 Adapter를 직접 참조하지 않는다")
  void controllersDependOnServices() throws Exception {
    // given
    var controllers = classes("**/controller/*Controller.class");

    // expected
    assertThat(controllers).hasSize(7);
    for (Class<?> controller : controllers) {
      var dependencies = Arrays.stream(controller.getDeclaredFields())
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .map(field -> field.getType().getPackageName()).toList();
      assertThat(dependencies).as(controller.getSimpleName()).isNotEmpty()
          .allMatch(packageName -> packageName.endsWith(".service"));
    }
  }

  @Test
  @DisplayName("Adapter의 입력과 반환 타입은 HTTP DTO에 의존하지 않는다")
  void adaptersDoNotExposeHttpDtos() throws Exception {
    // given
    var adapters = classes("**/adapter/*Adapter.class");

    // expected
    assertThat(adapters).hasSize(7);
    for (Class<?> adapter : adapters) {
      for (var method : adapter.getDeclaredMethods()) {
        assertThat(method.toGenericString()).doesNotContain(".dto.", ".response.ApiResult");
      }
    }
  }

  @Test
  @DisplayName("Service는 응답 DTO를 반환하고 HTTP 응답 포장과 클라이언트 타입에 의존하지 않는다")
  void servicesReturnResponseDtos() throws Exception {
    // given
    var services = classes("**/service/*Service.class");

    // expected
    assertThat(services).hasSize(7);
    for (Class<?> service : services) {
      for (var method : service.getDeclaredMethods()) {
        assertThat(method.getGenericReturnType().getTypeName())
            .doesNotContain(".response.ApiResult", "org.springframework.http", "net.spy.memcached");
        if (Modifier.isPublic(method.getModifiers())) {
          assertThat(method.getGenericReturnType().getTypeName())
              .contains(".dto.").doesNotContain(".model.");
        }
      }
    }
  }

  @Test
  @DisplayName("내부 모델은 HTTP DTO와 클라이언트 타입에 의존하지 않는다")
  void modelsKeepInternalTypes() throws Exception {
    // given
    var models = classes("**/model/*.class");

    // expected
    assertThat(models).isNotEmpty();
    for (Class<?> model : models) {
      for (var field : model.getDeclaredFields()) {
        assertThat(field.getGenericType().getTypeName())
            .doesNotContain(".dto.", ".response.", "net.spy.memcached");
      }
      assertThat(model.getAnnotations()).isEmpty();
    }
  }

  private List<Class<?>> classes(String pattern) throws Exception {
    var resolver = new PathMatchingResourcePatternResolver();
    var readers = new CachingMetadataReaderFactory(resolver);
    var resources = resolver.getResources("classpath*:com/jam2in/arcus/http/" + pattern);
    var classes = new ArrayList<Class<?>>();
    for (var resource : resources) {
      String name = readers.getMetadataReader(resource).getClassMetadata().getClassName();
      classes.add(Class.forName(name, false, getClass().getClassLoader()));
    }
    return classes;
  }
}
