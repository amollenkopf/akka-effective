package effectiveakka.pattern.extra
import akka.actor.{ Actor, ActorRef }
import effectiveakka.pattern._

class AccountBalanceRetriever2(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef) extends Actor{
  val checkingBalances, savingBalances, moneyMarketBalances: Option[List[(Long, BigDecimal)]] = None
  var originalSender: Option[ActorRef] = None
  
  def receive = {
    case GetCustomerAccountBalances(id) =>
      originalSender = Some(sender)
      cActor ! GetCustomerAccountBalances(id)
      sActor ! GetCustomerAccountBalances(id)
      mmActor ! GetCustomerAccountBalances(id)
    case AccountBalances(cBalances, sBalances, mmBalances) =>
      (checkingBalances, savingBalances, moneyMarketBalances) match {
        case (Some(c), Some(s), Some(mm)) =>
           originalSender.get ! AccountBalances(checkingBalances, savingBalances, moneyMarketBalances)
        case _ =>
      }
  }
}