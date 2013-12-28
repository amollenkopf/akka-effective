package effectiveakka.pattern.extra
import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.event.LoggingReceive
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AccountBalanceRetrieverFinal {
  case object AccountRetrievalTimeout
}

class AccountBalanceRetrieverFinal(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef) extends Actor
  with ActorLogging {
  import AccountBalanceRetrieverFinal._
  implicit val ec: ExecutionContext = context.dispatcher
  
  def receive = {
    case GetCustomerAccountBalances(id) =>
      log.debug(s"[ABR] Received GetCustomerAccountBalances for id: $id from $sender")
      val originalSender = sender
      
      context.actorOf(Props(new Actor() {
        var checkingBalances, savingBalances, moneyMarketBalances: Option[List[(Long, BigDecimal)]] = None
        def receive = LoggingReceive {
          case CheckingAccountBalances(balances) =>
            log.debug(s"[ABR] Received checking account balances: $balances")
            checkingBalances = balances
            collectBalances
          case SavingAccountBalances(balances) =>
            log.debug(s"[ABR] Received saving account balances: $balances")
            savingBalances = balances
            collectBalances
          case MoneyMarketAccountBalances(balances) =>
            log.debug(s"[ABR] Received money market account balances: $balances")
            moneyMarketBalances = balances
            collectBalances
          case AccountRetrievalTimeout =>
            sendResponseAndShutdown(AccountRetrievalTimeout)
        }

        def collectBalances =
          (checkingBalances, savingBalances, moneyMarketBalances) match {
            case (Some(c), Some(s), Some(mm)) =>
              log.debug(s"[ABR] Values recieved for all three account types")
              timeoutMessager.cancel
              sendResponseAndShutdown(AccountBalances(checkingBalances, savingBalances, moneyMarketBalances))
            case _ =>
          }

        def sendResponseAndShutdown(response: Any) = {
          originalSender ! response
          log.debug("[ABR] Stopping actor.")
          context.stop(self)
        }
        
        cActor ! GetCustomerAccountBalances(id)
        sActor ! GetCustomerAccountBalances(id)
        mmActor ! GetCustomerAccountBalances(id)

        val timeoutMessager = context.system.scheduler.scheduleOnce(250 milliseconds) {
          self ! AccountRetrievalTimeout
        }
      }))
  }
}