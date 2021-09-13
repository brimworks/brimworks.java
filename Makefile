test:
	gradle test jacocoTestCoverageVerification && open ./nio/build/reports/jacoco/test/html/index.html