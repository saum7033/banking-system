import org.springframework.web.util.pattern.PathPatternParser;
var p = new PathPatternParser();
String[] patterns = {
  "/api/v1/transaction/{transactionId}",
  "/api/v1/transaction/account/{accountNumber}",
  "/api/v1/transaction/{transactionId}/verify",
  "/api/v1/transaction/transfer",
  "/{transactionId}"
};
for (String s : patterns) {
  try { p.parse(s); System.out.println("OK   " + s); }
  catch (Exception e) { System.out.println("FAIL " + s + " -> " + e.getMessage()); }
}
/exit
