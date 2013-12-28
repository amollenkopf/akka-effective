package effectiveakka.pattern.cameo
import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.event.LoggingReceive
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import effectiveakka.pattern._

object AccountBalanceResponseHandler {
  case object AccountRetrievalTimeout
  def props(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef, originalSender: ActorRef): Props = {
    Props(new AccountBalanceResponseHandler(cActor, sActor, mmActor, originalSender))
  }
}

class AccountBalanceResponseHandler(
  cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef, originalSender: ActorRef)
  extends Actor with ActorLogging {
  import AccountBalanceResponseHandler._
  implicit val ec: ExecutionContext = context.dispatcher
  var checkingBalances, savingBalances, moneyMarketBalances: Option[List[(Long, BigDecimal)]] = None

  def receive = LoggingReceive {
    case CheckingAccountBalances(balances) =>
      log.debug(s"[ABRC] Received checking account balances: $balances")
      checkingBalances = balances
      collectBalances
    case SavingAccountBalances(balances) =>
      log.debug(s"[ABRC] Received saving account balances: $balances")
      savingBalances = balances
      collectBalances
    case MoneyMarketAccountBalances(balances) =>
      log.debug(s"[ABRC] Received money market account balances: $balances")
      moneyMarketBalances = balances
      collectBalances
    case AccountRetrievalTimeout =>
      log.debug("[ABRC] Timeout occurred")
      sendResponseAndShutdown(AccountRetrievalTimeout)
  }

  def collectBalances =
    (checkingBalances, savingBalances, moneyMarketBalances) match {
      case (Some(c), Some(s), Some(mm)) =>
        log.debug(s"[ABRC] Values recieved for all three account types")
        timeoutMessager.cancel
        sendResponseAndShutdown(AccountBalances(checkingBalances, savingBalances, moneyMarketBalances))
      case _ =>
    }

  def sendResponseAndShutdown(response: Any) = {
    originalSender ! response
    log.debug("[ABR] Stopping actor.")
    context.stop(self)
  }

  val timeoutMessager = context.system.scheduler.scheduleOnce(250 milliseconds) {
    self ! AccountRetrievalTimeout
  }
}

class AccountBalanceRetrieverCameo(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef) extends Actor {
  def receive = {
    case GetCustomerAccountBalances(id) =>
      val originalSender = sender //must freeze value here, not safe to embed as parameter on the next line
      val handler = context.actorOf(AccountBalanceResponseHandler.props(cActor, sActor, mmActor, originalSender), "cameo-message-handler")
      cActor.tell(GetCustomerAccountBalances(id), handler)
      sActor.tell(GetCustomerAccountBalances(id), handler)
      mmActor.tell(GetCustomerAccountBalances(id), handler)
  }
}