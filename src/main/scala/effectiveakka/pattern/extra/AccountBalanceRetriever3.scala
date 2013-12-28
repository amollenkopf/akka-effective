package effectiveakka.pattern.extra
import akka.actor.{ Actor, ActorRef, Props }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import effectiveakka.pattern._

class AccountBalanceRetriever3(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef) extends Actor {
  val checkingBalances, savingBalances, moneyMarketBalances: Option[List[(Long, BigDecimal)]] = None

  def receive = {
    case GetCustomerAccountBalances(id) =>
      context.actorOf(Props(new Actor() {
        var checkingBalances, savingBalances, moneyMarketBalances: Option[List[(Long, BigDecimal)]] = None
        val originalSender = sender
        def receive = {
          case CheckingAccountBalances(balances) =>
            checkingBalances = balances
            isDone
          case SavingAccountBalances(balances) =>
            savingBalances = balances
            isDone
          case MoneyMarketAccountBalances(balances) =>
            moneyMarketBalances = balances
            isDone
        }
        def isDone =
          (checkingBalances, savingBalances, moneyMarketBalances) match {
            case (Some(c), Some(s), Some(mm)) =>
              originalSender ! AccountBalances(checkingBalances, savingBalances, moneyMarketBalances)
              context.stop(self)
            case _ =>
          }
        cActor ! GetCustomerAccountBalances(id)
        sActor ! GetCustomerAccountBalances(id)
        mmActor ! GetCustomerAccountBalances(id)
      }))

  }
}