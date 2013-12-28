package effectiveakka.pattern
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import effectiveakka.pattern._

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