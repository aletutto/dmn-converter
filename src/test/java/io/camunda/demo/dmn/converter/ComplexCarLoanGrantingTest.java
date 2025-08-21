package io.camunda.demo.dmn.converter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.EvaluateDecisionResponse;
import io.camunda.zeebe.spring.client.annotation.Deployment;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@CamundaSpringProcessTest
@Deployment(resources = "ComplexCarLoanGranting.dmn")
class ComplexCarLoanGrantingTest {
  @Autowired private ZeebeClient client;


  @Test
  void testLoanGranted() {
    final String granted = "Granted";
    assertAll(
        () -> testLoanGranting(400, 80, granted), // Affordable - low
        () -> testLoanGranting(800, 95, granted), // Marginal - low
        () -> testLoanGranting(500, 91, granted)); // Affordable - medium
  }

  @Test
  void testLoanRejected() {
    final String rejected = "Rejected";
    assertAll(
        () -> testLoanGranting(2000, 100, rejected), // Unaffordable - low
        () -> testLoanGranting(2000, 92, rejected), // Unaffordable - medium
        () -> testLoanGranting(2000, 56, rejected), // Unaffordable - high
        () -> testLoanGranting(400, 79, rejected), // Affordable - high
        () -> testLoanGranting(800, 79, rejected), // Marginal - high
        () -> testLoanGranting(2000, 79, rejected), // Unaffordable - high
        () -> testLoanGranting(800, 93, rejected)); // Marginal - medium
  }

  private void testLoanGranting(int installmentRate, int creditScore, String expected) {
    // given
    var variables = new HashMap<String, Object>();
    variables.put("installmentRate", installmentRate);
    variables.put("borrowersIncome", 2000);
    variables.put("expenses", 800);
    variables.put("creditScore", creditScore);

    // when
    EvaluateDecisionResponse decisionResponse =
        client
            .newEvaluateDecisionCommand()
            .decisionId("carLoanGranting")
            .variables(variables)
            .send()
            .join();

    // then
    assertEquals(expected, decisionResponse.getDecisionOutput().replace("\"", ""));
  }
}
