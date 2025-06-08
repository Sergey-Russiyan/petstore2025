package com.petstore.framework;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class AllureTestListener implements ITestListener {
  @Override
  public void onTestFailure(ITestResult result) {
    Allure.getLifecycle().updateTestCase(testResult -> testResult.setStatus(Status.FAILED));
  }
}
