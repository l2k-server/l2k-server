package org.l2kserver.game.handler

import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.AuthorizationRequest
import org.l2kserver.game.handler.dto.request.CancelActionRequest
import org.l2kserver.game.handler.dto.request.CharacterTemplatesRequest
import org.l2kserver.game.handler.dto.request.CreateCharacterRequest
import org.l2kserver.game.handler.dto.request.DeleteCharacterRequest
import org.l2kserver.game.handler.dto.request.DeleteItemRequest
import org.l2kserver.game.handler.dto.request.TakeOffItemRequest
import org.l2kserver.game.handler.dto.request.EnterWorldRequest
import org.l2kserver.game.handler.dto.request.InitialRequest
import org.l2kserver.game.handler.dto.request.LogoutRequest
import org.l2kserver.game.handler.dto.request.ManorListRequest
import org.l2kserver.game.handler.dto.request.MoveRequest
import org.l2kserver.game.handler.dto.request.RequestPacket
import org.l2kserver.game.handler.dto.request.RestartRequest
import org.l2kserver.game.handler.dto.request.RestoreCharacterRequest
import org.l2kserver.game.handler.dto.request.SelectCharacterRequest
import org.l2kserver.game.handler.dto.request.ActionRequest
import org.l2kserver.game.handler.dto.request.AdminCommandRequest
import org.l2kserver.game.handler.dto.request.AttackRequest
import org.l2kserver.game.handler.dto.request.BasicActionRequest
import org.l2kserver.game.handler.dto.request.ChatMessageRequest
import org.l2kserver.game.handler.dto.request.DeleteShortcutRequest
import org.l2kserver.game.handler.dto.request.DropItemRequest
import org.l2kserver.game.handler.dto.request.CreateShortcutRequest
import org.l2kserver.game.handler.dto.request.ItemListForPrivateStoreBuyRequest
import org.l2kserver.game.handler.dto.request.ItemListForPrivateStoreSellRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreBuySetMessageRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreBuyStartRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreBuyStopRequest
import org.l2kserver.game.handler.dto.request.BuyInPrivateStoreRequest
import org.l2kserver.game.handler.dto.request.ExchangeRequest
import org.l2kserver.game.handler.dto.request.RespawnRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellSetMessageRequest
import org.l2kserver.game.handler.dto.request.ShowMapRequest
import org.l2kserver.game.handler.dto.request.SkillListRequest
import org.l2kserver.game.handler.dto.request.SocialActionRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellStartRequest
import org.l2kserver.game.handler.dto.request.PrivateStoreSellStopRequest
import org.l2kserver.game.handler.dto.request.SellToPrivateStoreRequest
import org.l2kserver.game.handler.dto.request.UseItemRequest
import org.l2kserver.game.handler.dto.request.UseSkillRequest
import org.l2kserver.game.handler.dto.request.UserCommandRequest
import org.l2kserver.game.handler.dto.request.ValidatePositionRequest
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.ManorListResponse
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.service.ActionService
import org.l2kserver.game.service.AdminCommandService
import org.l2kserver.game.service.AuthorizationService
import org.l2kserver.game.service.CharacterService
import org.l2kserver.game.service.ItemService
import org.l2kserver.game.service.MoveService
import org.l2kserver.game.service.ShortcutService
import org.l2kserver.game.service.SkillService
import org.l2kserver.game.service.SocialService
import org.l2kserver.game.service.TradeService
import org.l2kserver.game.service.UserCommandService
import org.springframework.stereotype.Component

@Component
class L2GameRequestHandler(
    private val authorizationService: AuthorizationService,
    private val characterService: CharacterService,
    private val actionService: ActionService,
    private val itemService: ItemService,
    private val moveService: MoveService,
    private val adminCommandService: AdminCommandService,
    private val userCommandService: UserCommandService,
    private val socialService: SocialService,
    private val shortcutService: ShortcutService,
    private val skillService: SkillService,
    private val tradeService: TradeService
) {

    private val log = logger()

    suspend fun handle(key: ByteArray, request: RequestPacket?) {
        log.debug("Got request {}", request)
        when (request) {
            is InitialRequest -> authorizationService.checkProtocolVersion(request, key)
            is AuthorizationRequest -> authorizationService.authorize(request)

            is CharacterTemplatesRequest -> characterService.getCharacterTemplates()
            is CreateCharacterRequest -> characterService.createCharacter(request)
            is DeleteCharacterRequest -> characterService.deleteCharacter(request)
            is RestoreCharacterRequest -> characterService.restoreCharacter(request)
            is SelectCharacterRequest -> characterService.selectCharacter(request)
            is EnterWorldRequest -> characterService.enterWorld()
            is LogoutRequest -> characterService.exitGame()
            is RestartRequest -> characterService.exitToCharactersMenu()
            is RespawnRequest -> characterService.respawnCharacter(request)

            is ManorListRequest -> send(ManorListResponse)

            is UseItemRequest -> itemService.useItem(request)
            is TakeOffItemRequest -> itemService.takeOffItem(request)
            is DeleteItemRequest -> itemService.deleteItem(request)
            is DropItemRequest -> itemService.dropItem(request)

            is MoveRequest -> moveService.moveCharacter(request)
            is ValidatePositionRequest -> moveService.validatePosition(request)

            is ActionRequest -> actionService.performAction(request)
            is CancelActionRequest -> actionService.cancelAction()
            is AttackRequest -> actionService.attackTarget(request)
            is ShowMapRequest -> actionService.showMap()

            is AdminCommandRequest -> adminCommandService.handleAdminCommand(request)
            is UserCommandRequest -> userCommandService.handleUserCommand(request)

            is BasicActionRequest -> actionService.performBasicAction(request)
            is SocialActionRequest -> actionService.performSocialAction(request)
            is ChatMessageRequest -> socialService.handleChatMessageRequest(request)

            is CreateShortcutRequest -> shortcutService.registerShortcut(request)
            is DeleteShortcutRequest -> shortcutService.deleteShortcut(request)

            is SkillListRequest -> skillService.getSkillList()
            is UseSkillRequest -> skillService.useSkill(request)

            is ExchangeRequest -> tradeService.startExchanging(request)

            is ItemListForPrivateStoreSellRequest -> tradeService.getItemsForPrivateStoreSell()
            is PrivateStoreSellStartRequest -> tradeService.startPrivateStoreSell(request)
            is PrivateStoreSellSetMessageRequest -> tradeService.setPrivateStoreSellMessage(request)
            is BuyInPrivateStoreRequest -> tradeService.buyInPrivateStore(request)

            is ItemListForPrivateStoreBuyRequest -> tradeService.getItemsForPrivateStoreBuy()
            is PrivateStoreBuyStartRequest -> tradeService.startPrivateStoreBuy(request)
            is PrivateStoreBuySetMessageRequest -> tradeService.setPrivateStoreBuyMessage(request)
            is SellToPrivateStoreRequest -> tradeService.sellToPrivateStore(request)

            is PrivateStoreSellStopRequest, is PrivateStoreBuyStopRequest -> tradeService.stopPrivateStore()

            else -> send(ActionFailedResponse)
        }
    }

    suspend fun handleDisconnect() {
        authorizationService.logOut()
        characterService.disconnectGame()

        sessionContext().close()
    }

}
