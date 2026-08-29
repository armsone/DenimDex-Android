package com.armsone.denimdex.core.design

/**
 * Stable Compose test tag identifiers across DenimDex.
 * Enables deterministic UI discovery and headless/device screenshot capture.
 */
object DenimTestTags {
    // Top-level Navigation Tabs
    const val TAB_SCAN = "tab_scan"
    const val TAB_ARCHIVE = "tab_archive"
    const val TAB_GUIDE = "tab_guide"
    const val TAB_SETTINGS = "tab_settings"

    // Scan Screen
    const val SCAN_SCREEN = "scan_screen"
    const val SCAN_HEADER_CARD = "scan_header_card"
    const val SCAN_PHOTO_COLLECTOR = "scan_photo_collector"
    const val SCAN_PHOTO_COUNT_BADGE = "scan_photo_count_badge"
    const val SCAN_CLEAR_ALL_PHOTOS_BUTTON = "scan_clear_all_photos_button"
    const val SCAN_PHOTO_GRID = "scan_photo_grid"
    fun scanPhotoThumbnail(index: Int) = "scan_photo_thumbnail_$index"
    fun scanPhotoDeleteButton(index: Int) = "scan_photo_delete_button_$index"
    const val SCAN_ADD_PHOTO_TILE = "scan_add_photo_tile"
    const val SCAN_CAMERA_BUTTON = "scan_camera_button"
    const val SCAN_PHOTO_PICKER_BUTTON = "scan_photo_picker_button"
    const val SCAN_START_BUTTON = "scan_start_button"
    const val SCAN_EMPTY_HINT = "scan_empty_hint"
    const val SCAN_RUNNING_PANEL = "scan_running_panel"
    const val SCAN_RUNNING_STATUS_MESSAGE = "scan_running_status_message"
    const val SCAN_RUNNING_COUNTDOWN = "scan_running_countdown"
    const val SCAN_RUNNING_CANCEL_BUTTON = "scan_running_cancel_button"
    const val SCAN_RUNNING_PHOTO_SUMMARY = "scan_running_photo_summary"
    const val SCAN_RESULT_CARD = "scan_result_card"
    const val SCAN_RESULT_CONFIDENCE_BADGE = "scan_result_confidence_badge"
    const val SCAN_RESULT_TITLE = "scan_result_title"
    const val SCAN_RESULT_ERA = "scan_result_era"
    const val SCAN_RESULT_SUMMARY = "scan_result_summary"
    const val SCAN_RESULT_KOREA_CARD = "scan_result_korea_card"
    const val SCAN_RESULT_JAPAN_CARD = "scan_result_japan_card"
    const val SCAN_RESULT_KOREA_SALE_RANGE = "scan_result_korea_sale_range"
    const val SCAN_RESULT_KOREA_NET_PROCEEDS = "scan_result_korea_net_proceeds"
    const val SCAN_RESULT_JAPAN_SALE_RANGE = "scan_result_japan_sale_range"
    const val SCAN_RESULT_JAPAN_NET_PROCEEDS = "scan_result_japan_net_proceeds"
    const val SCAN_RESULT_CONDITION = "scan_result_condition"
    const val SCAN_RESULT_VALUE_REASONS = "scan_result_value_reasons"
    const val SCAN_RESULT_ARBITRAGE_SECTION = "scan_result_arbitrage_section"
    const val SCAN_RESULT_J2K_MARGIN = "scan_result_j2k_margin"
    const val SCAN_RESULT_K2J_MARGIN = "scan_result_k2j_margin"
    const val SCAN_RESULT_ARBITRAGE_RECOMMENDATION = "scan_result_arbitrage_recommendation"
    const val SCAN_RESULT_DISCLAIMER = "scan_result_disclaimer"
    const val SCAN_RESULT_CAVEATS = "scan_result_caveats"
    const val SCAN_RESULT_SAVE_BUTTON = "scan_result_save_button"
    const val SCAN_RESULT_NEXT_INSTRUCTION_BUTTON = "scan_result_next_instruction_button"
    const val SCAN_RESULT_RESTART_BUTTON = "scan_result_restart_button"
    const val SCAN_ERROR_PANEL = "scan_error_panel"
    const val SCAN_ERROR_MESSAGE = "scan_error_message"
    const val SCAN_ERROR_RETRY_BUTTON = "scan_error_retry_button"
    const val SCAN_TIMEOUT_PANEL = "scan_timeout_panel"
    const val SCAN_TIMEOUT_RETRY_BUTTON = "scan_timeout_retry_button"

    // Camera Capture Screen
    const val CAMERA_SCREEN = "camera_screen"
    const val CAMERA_CANCEL_BUTTON = "camera_cancel_button"
    const val CAMERA_COUNT_BADGE = "camera_count_badge"
    const val CAMERA_DONE_BUTTON = "camera_done_button"
    const val CAMERA_SWITCH_LENS_BUTTON = "camera_switch_lens_button"
    const val CAMERA_SHUTTER_BUTTON = "camera_shutter_button"
    const val CAMERA_FLASH_BUTTON = "camera_flash_button"

    // Archive Screen
    const val ARCHIVE_SCREEN = "archive_screen"
    const val ARCHIVE_TITLE = "archive_title"
    const val ARCHIVE_SEARCH_FIELD = "archive_search_field"
    const val ARCHIVE_EMPTY_VIEW = "archive_empty_view"
    const val ARCHIVE_LIST = "archive_list"
    fun archiveItemRow(id: String) = "archive_item_$id"
    const val ARCHIVE_ITEM_TITLE = "archive_item_title"
    const val ARCHIVE_ITEM_VALUE_RANGE = "archive_item_value_range"
    const val ARCHIVE_ITEM_VERIFICATION_BADGE = "archive_item_verification_badge"

    // Archive Detail Screen
    const val ARCHIVE_DETAIL_SCREEN = "archive_detail_screen"
    const val ARCHIVE_DETAIL_BACK_BUTTON = "archive_detail_back_button"
    const val ARCHIVE_DETAIL_DELETE_ICON_BUTTON = "archive_detail_delete_icon_button"
    const val ARCHIVE_DETAIL_PHOTO_CAROUSEL = "archive_detail_photo_carousel"
    const val ARCHIVE_DETAIL_TITLE_FIELD = "archive_detail_title_field"
    const val ARCHIVE_DETAIL_VALUE_CARD = "archive_detail_value_card"
    const val ARCHIVE_DETAIL_ATTRIBUTE_GRID = "archive_detail_attribute_grid"
    const val ARCHIVE_DETAIL_SUMMARY = "archive_detail_summary"
    const val ARCHIVE_DETAIL_VERIFICATION_SELECTOR = "archive_detail_verification_selector"
    const val ARCHIVE_DETAIL_NOTES_FIELD = "archive_detail_notes_field"
    const val ARCHIVE_DETAIL_DELETE_BUTTON = "archive_detail_delete_button"

    // Guide Screen
    const val GUIDE_SCREEN = "guide_screen"
    const val GUIDE_TITLE = "guide_title"
    const val GUIDE_BANNER = "guide_banner"
    const val GUIDE_SECTION_COUNT = "guide_section_count"
    const val GUIDE_SECTION_SHOOTING = "guide_section_shooting"
    const val GUIDE_SECTION_DETAILS = "guide_section_details"
    const val GUIDE_SECTION_CONDITION = "guide_section_condition"
    const val GUIDE_SECTION_CONFIDENCE = "guide_section_confidence"
    const val GUIDE_SECTION_VALUE_BASIS = "guide_section_value_basis"
    const val GUIDE_SECTION_DISCLAIMER = "guide_section_disclaimer"

    // Settings Screen
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_TITLE = "settings_title"
    const val SETTINGS_HEADER_BANNER = "settings_header_banner"
    const val SETTINGS_CHATGPT_ROW = "settings_chatgpt_row"
    const val SETTINGS_CHATGPT_STATUS_BADGE = "settings_chatgpt_status_badge"
    const val SETTINGS_CLEAR_SESSION_BUTTON = "settings_clear_session_button"
    const val SETTINGS_SYNC_SECTION = "settings_sync_section"
    const val SETTINGS_PRIVACY_SECTION = "settings_privacy_section"
    const val SETTINGS_CLEAR_ARCHIVE_BUTTON = "settings_clear_archive_button"
    const val SETTINGS_VERSION_SECTION = "settings_version_section"
    const val SETTINGS_VERSION_VALUE = "settings_version_value"
    const val SETTINGS_BUILD_VALUE = "settings_build_value"

    // Dialogs & Sheets
    const val DIALOG_CONSENT = "dialog_consent"
    const val DIALOG_CONSENT_CONFIRM_BUTTON = "dialog_consent_confirm_button"
    const val DIALOG_CONSENT_CANCEL_BUTTON = "dialog_consent_cancel_button"

    const val DIALOG_CLEAR_ALL_PHOTOS = "dialog_clear_all_photos"
    const val DIALOG_CLEAR_ALL_CONFIRM_BUTTON = "dialog_clear_all_confirm_button"
    const val DIALOG_CLEAR_ALL_CANCEL_BUTTON = "dialog_clear_all_cancel_button"

    const val DIALOG_PHOTO_SAVE_ALERT = "dialog_photo_save_alert"
    const val DIALOG_PHOTO_SAVE_CONFIRM_BUTTON = "dialog_photo_save_confirm_button"

    const val DIALOG_DELETE_ITEM = "dialog_delete_item"
    const val DIALOG_DELETE_ITEM_CONFIRM_BUTTON = "dialog_delete_item_confirm_button"
    const val DIALOG_DELETE_ITEM_CANCEL_BUTTON = "dialog_delete_item_cancel_button"

    const val DIALOG_CLEAR_SESSION = "dialog_clear_session"
    const val DIALOG_CLEAR_SESSION_CONFIRM_BUTTON = "dialog_clear_session_confirm_button"
    const val DIALOG_CLEAR_SESSION_CANCEL_BUTTON = "dialog_clear_session_cancel_button"

    const val DIALOG_SESSION_CLEARED = "dialog_session_cleared"
    const val DIALOG_SESSION_CLEARED_CONFIRM_BUTTON = "dialog_session_cleared_confirm_button"

    const val DIALOG_CLEAR_ARCHIVE = "dialog_clear_archive"
    const val DIALOG_CLEAR_ARCHIVE_CONFIRM_BUTTON = "dialog_clear_archive_confirm_button"
    const val DIALOG_CLEAR_ARCHIVE_CANCEL_BUTTON = "dialog_clear_archive_cancel_button"

    const val SHEET_SYNC_INVITE = "sheet_sync_invite"
    const val SHEET_SYNC_INVITE_START_BUTTON = "sheet_sync_invite_start_button"
    const val SHEET_SYNC_INVITE_LATER_BUTTON = "sheet_sync_invite_later_button"
    const val SHEET_SYNC_INVITE_DISCLOSURE_BUTTON = "sheet_sync_invite_disclosure_button"

    const val SHEET_SYNC_DISCLOSURE = "sheet_sync_disclosure"
    const val SHEET_SYNC_DISCLOSURE_CLOSE_BUTTON = "sheet_sync_disclosure_close_button"

    const val SHEET_AIBI_LOGIN = "sheet_aibi_login"
    const val SHEET_AIBI_LOGIN_CLOSE_BUTTON = "sheet_aibi_login_close_button"

    const val SHEET_AIBI_VISIBLE_BROWSER = "sheet_aibi_visible_browser"
    const val SHEET_AIBI_VISIBLE_CANCEL_BUTTON = "sheet_aibi_visible_cancel_button"
    const val SHEET_AIBI_VISIBLE_MENU_BUTTON = "sheet_aibi_visible_menu_button"

    const val DIALOG_TV_EXIT = "dialog_tv_exit"
    const val DIALOG_TV_EXIT_CONFIRM_BUTTON = "dialog_tv_exit_confirm_button"
    const val DIALOG_TV_EXIT_CANCEL_BUTTON = "dialog_tv_exit_cancel_button"
}
