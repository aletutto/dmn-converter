package io.camunda.demo.dmn.converter;

import java.io.File;
import java.util.Map;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputEntry;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.camunda.bpm.model.dmn.instance.Text;
import org.camunda.feel.FeelEngine;
import org.camunda.feel.FeelEngine.Failure;
import org.camunda.feel.impl.SpiServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import scala.util.Either;

@SpringBootApplication
public class DmnConverterDemoApplication {

  private static final Logger log = LoggerFactory.getLogger(DmnConverterDemoApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(DmnConverterDemoApplication.class, args);
    createDmn();
    // validateFaultyDmn();
  }

  private static void createDmn() {
    // Create a new DMN model instance
    DmnModelInstance dmnModelInstance = Dmn.createEmptyModel();
    // Create definitions element
    Definitions definitions = dmnModelInstance.newInstance(Definitions.class);
    definitions.setName("DRD");
    definitions.setNamespace("http://camunda.org/schema/1.0/dmn");
    definitions.setAttributeValueNs(
        "http://camunda.org/schema/modeler/1.0", "modeler:executionPlatform", "Camunda Cloud");
    definitions.setAttributeValueNs(
        "http://camunda.org/schema/modeler/1.0", "modeler:executionPlatformVersion", "8.6.0");
    dmnModelInstance.setDocumentElement(definitions);

    // Create decision element
    Decision decision = dmnModelInstance.newInstance(Decision.class);
    decision.setName("Sample Decision");
    decision.setId("sampleDecision");
    definitions.addChildElement(decision);

    // Create decision table element
    DecisionTable decisionTable = dmnModelInstance.newInstance(DecisionTable.class);
    decisionTable.setHitPolicy(HitPolicy.UNIQUE);
    decision.addChildElement(decisionTable);

    // Create input element
    Input input = dmnModelInstance.newInstance(Input.class);
    input.setId("temperature");
    input.setLabel("Temperature");
    decisionTable.addChildElement(input);

    // Create input expression element
    InputExpression inputExpression = dmnModelInstance.newInstance(InputExpression.class);
    inputExpression.setTypeRef("number");
    Text text = dmnModelInstance.newInstance(Text.class);
    text.setTextContent("temperature");
    inputExpression.setText(text);
    input.addChildElement(inputExpression);

    // Create output element
    Output output = dmnModelInstance.newInstance(Output.class);
    output.setName("climate");
    output.setLabel("Climate");
    output.setTypeRef("string");
    decisionTable.addChildElement(output);

    // Add a new rule
    Rule rule = dmnModelInstance.newInstance(Rule.class);

    InputEntry newInputEntry = dmnModelInstance.newInstance(InputEntry.class);
    newInputEntry.setText(dmnModelInstance.newInstance(Text.class));
    newInputEntry.getText().setTextContent("< 15 !");
    rule.addChildElement(newInputEntry);

    OutputEntry newOutputEntry = dmnModelInstance.newInstance(OutputEntry.class);
    newOutputEntry.setText(dmnModelInstance.newInstance(Text.class));
    newOutputEntry.getText().setTextContent("\"Warm\"");
    rule.addChildElement(newOutputEntry);

    decisionTable.addChildElement(rule);

    // Write the DMN model to the specified file
    File file = new File("./src/main/resources/GeneratedDmn.dmn");
    Dmn.writeModelToFile(file, dmnModelInstance);
  }

  private static void validateFaultyDmn() {
    // Validate faulty DMN
    File faultyDmnFile = new File("./src/main/resources/Faulty.dmn");
    final DmnModelInstance faultyDmn = Dmn.readModelFromFile(faultyDmnFile);
    Dmn.validateModel(faultyDmn);

    final FeelEngine engine =
        new FeelEngine.Builder()
            .valueMapper(SpiServiceLoader.loadValueMapper())
            .functionProvider(SpiServiceLoader.loadFunctionProvider())
            .build();

    final Map<String, Object> variables = Map.of("x", 21);
    final Either<Failure, Object> result = engine.evalExpression("> 15", variables);

    if (result.isRight()) {
      final Object value = result.right().get();
      log.info("result is " + value);
    } else {
      final FeelEngine.Failure failure = result.left().get();
      throw new RuntimeException(failure.message());
    }
  }
}
