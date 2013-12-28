package effectiveakka.pattern.extra
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.event.LoggingReceive
import scala.concurrent.duration._
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import effectiveakka.pattern.extra._

class CheckingAccountsProxyStub extends CheckingAccountsProxy with ActorLogging {
  val accounts = Map[Long, List[(Long, BigDecimal)]](
    1l -> List((3, 15000)),
    2l -> List((6, 640000), (7, 1125000), (8, 40000)))
  def receive = LoggingReceive {
    case GetCustomerAccountBalances(id: Long) =>
      log.debug(s"[CAPS] Received GetCustomerAccountBalances for id: $id")
      accounts.get(id) match {
        case Some(accountList) => sender ! CheckingAccountBalances(Some(accountList))
        case None => sender ! CheckingAccountBalances(Some(List()))
      }
  }
}

class SavingAccountsProxyStub extends SavingAccountsProxy with ActorLogging {
  val accounts = Map[Long, List[(Long, BigDecimal)]](
    1L -> (List((1, 150000), (2, 29000))),
    2L -> (List((5, 80000))))
  def receive = LoggingReceive {
    case GetCustomerAccountBalances(id: Long) =>
      log.debug(s"[SAPS] Received GetCustomerAccountBalances for id: $id")
      accounts.get(id) match {
        case Some(accountList) => sender ! SavingAccountBalances(Some(accountList))
        case None => sender ! SavingAccountBalances(Some(List()))
      }
  }
}

class MoneyMarketAccountsProxyStub extends MoneyMarketAccountsProxy with ActorLogging {
  val accounts = Map[Long, List[(Long, BigDecimal)]](
    2L -> List((9, 640000), (10, 1125000), (11, 40000)))
  def receive = LoggingReceive {
    case GetCustomerAccountBalances(id: Long) =>
      log.debug(s"[MMAPS] Received GetCustomerAccountBalances for id: $id")
      accounts.get(id) match {
        case Some(accountList) => sender ! MoneyMarketAccountBalances(Some(accountList))
        case None => sender ! MoneyMarketAccountBalances(Some(List()))
      }
  }
}

class TimingOutSavingsAccountProxyStub extends SavingAccountsProxy with ActorLogging {
  def receive = LoggingReceive {
    case GetCustomerAccountBalances(id: Long) =>
      log.debug(s"[TOSAPS] Forcing timeout by not responding!")
  }
}

class AccountBalanceRetrieverSpec extends TestKit(ActorSystem("AccountBalanceRetrieverSystem"))
  with ImplicitSender with WordSpec with MustMatchers {

  "An AccountBalanceRetriever" should {
    "return a valid list of account balances" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      val cProxy = system.actorOf(Props[CheckingAccountsProxyStub], "checkings-success")
      val sProxy = system.actorOf(Props[SavingAccountsProxyStub], "savings-success")
      val mmProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "moneyMarkets-success")
      val abRetriever = system.actorOf(Props(new AccountBalanceRetrieverFinal(cProxy, sProxy, mmProxy)), "accountBalanceRetriever-success")
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
      val cProxy = system.actorOf(Props[CheckingAccountsProxyStub], "checkings-timeout")
      val sProxy = system.actorOf(Props[TimingOutSavingsAccountProxyStub], "savings-timeout") //using TimingOut...Proxy
      val mmProxy = system.actorOf(Props[MoneyMarketAccountsProxyStub], "moneyMarkets-timeout")
      val abRetriever = system.actorOf(Props(new AccountBalanceRetrieverFinal(cProxy, sProxy, mmProxy)), "accountBalanceRetriever-timeout")
      val probe = TestProbe()
      within(250 milliseconds, 500 milliseconds) {
        probe.send(abRetriever, GetCustomerAccountBalances(1L))
        probe.expectMsg(AccountBalanceRetrieverFinal.AccountRetrievalTimeout)
      }
    }
  }
}