package effectiveakka.pattern.cameo
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.event.LoggingReceive
import scala.concurrent.duration._
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import effectiveakka.pattern._
import effectiveakka.pattern.cameo._

class AccountBalanceRetrieverCameoSpec extends TestKit(ActorSystem("AccountBalanceRetrieverCameoSystem"))
  with ImplicitSender with WordSpec with MustMatchers {

  "An AccountBalanceRetriever" should {
    "return a valid list of account balances" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val cProxy = system.actorOf(Props[CheckingAccountsProxyStub], "checkings-cameo-success")
      val sProxy = system.actorOf(Props[SavingAccountsProxyStub], "savings-cameo-success")
      val mmProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "moneyMarkets-cameo-success")
      val abRetriever = system.actorOf(Props(new AccountBalanceRetrieverCameo(cProxy, sProxy, mmProxy)), "accountBalanceRetriever-cameo-success")
      within(300 milliseconds) {
        probe1.send(abRetriever, GetCustomerAccountBalances(1L))
        val r = probe1.expectMsgType[AccountBalances]
        r must equal(
          AccountBalances(
            Some(List((3, 15000))),
            Some(List((1, 150000), (2, 29000))),
            Some(List())))
      }
      within(300 milliseconds) {
        probe2.send(abRetriever, GetCustomerAccountBalances(2L))
        val r = probe2.expectMsgType[AccountBalances]
        r must equal(
          AccountBalances(
            Some(List((6, 640000), (7, 1125000), (8, 40000))),
            Some(List((5, 80000))),
            Some(List((9, 640000), (10, 1125000), (11, 40000)))))
      }
    }

    "AccountBalanceRetriever sends a TimeoutException event when timeout occurs" in {
      val cProxy = system.actorOf(Props[CheckingAccountsProxyStub], "checkings-cameo-timeout")
      val sProxy = system.actorOf(Props[TimingOutSavingsAccountProxyStub], "savings-cameo-timeout") //using TimingOut...Proxy
      val mmProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "moneyMarkets-cameo-timeout")
      val abRetriever = system.actorOf(Props(new AccountBalanceRetrieverCameo(cProxy, sProxy, mmProxy)), "accountBalanceRetriever-cameo-timeout")
      val probe = TestProbe()
      within(250 milliseconds, 500 milliseconds) {
        probe.send(abRetriever, GetCustomerAccountBalances(1L))
        probe.expectMsg(AccountBalanceResponseHandler.AccountRetrievalTimeout)
      }
    }
  }
}