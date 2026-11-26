package com.synqt.sdk.internal.form

import com.synqt.sdk.PaymentMethodPayin
import com.synqt.sdk.R

/** One entry per direct API payment method a payer can fill in, shown by [PayinCheckout.presentPaymentSheet]. */
internal val METHOD_FORMS = listOf(
    MethodForm("ABSA_PAY", R.string.synqt_absa_pay, listOf(
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("passportNumber", R.string.synqt_passport_number, FieldKind.TEXT, required = false, hint = R.string.synqt_a_letter_followed_by_numbers, minLength = 8, maxLength = 9, pattern = "^[a-zA-Z][0-9]+\$", invalid = R.string.synqt_enter_a_letter_followed_by_7_or_8_numbers),
        FormField("said", R.string.synqt_south_african_id_number, FieldKind.NUMBER, required = false, hint = R.string.synqt_13_digits, minLength = 13, maxLength = 13, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_13_digits),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.TEXT, required = false),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    ), oneOfs = listOf(OneOf(R.string.synqt_how_do_you_want_to_confirm_it_s_you, listOf(Choice(R.string.synqt_bank_account, listOf("account.accountNumber", "account.accountName")), Choice(R.string.synqt_south_african_id_number, listOf("said")), Choice(R.string.synqt_passport_number, listOf("passportNumber")))))) { values ->
        PaymentMethodPayin.AbsaPayMethod(
            emailAddress = values.str("emailAddress"),
            passportNumber = values.str("passportNumber"),
            said = values.str("said"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountNumber = values.str("account.accountNumber"),
                    accountName = values.str("account.accountName"),
                )
            },
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.str("customerName.firstName"),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("CAPITEC_PAY", R.string.synqt_capitec_pay, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = false, placeholder = R.string.synqt_0821234567, maxLength = 12, pattern = "^0", invalid = R.string.synqt_enter_up_to_12_characters_starting_with_0),
        FormField("said", R.string.synqt_south_african_id_number, FieldKind.NUMBER, required = false, hint = R.string.synqt_13_digits, minLength = 13, maxLength = 13, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_13_digits),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.NUMBER, required = false, placeholder = R.string.synqt_10_digits, minLength = 10, maxLength = 10, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_10_digits),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    ), oneOfs = listOf(OneOf(R.string.synqt_how_do_you_want_to_confirm_it_s_you, listOf(Choice(R.string.synqt_bank_account, listOf("account.accountNumber", "account.accountName")), Choice(R.string.synqt_mobile_number, listOf("phoneNumber")), Choice(R.string.synqt_south_african_id_number, listOf("said")))))) { values ->
        PaymentMethodPayin.CapitecPayMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            said = values.str("said"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.str("customerName.firstName"),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("CODE_PAYMENT", R.string.synqt_payment_code, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = true, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("paymentLocation.postalCode", R.string.synqt_postal_code, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
        FormField("paymentLocation.city", R.string.synqt_city, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
        FormField("paymentLocation.district", R.string.synqt_district, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
    )) { values ->
        PaymentMethodPayin.CodePaymentMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            phoneNumber = values.req("phoneNumber", values.str("phoneNumber")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
            paymentLocation = PaymentMethodPayin.PaymentLocation(
                postalCode = values.req("paymentLocation.postalCode", values.str("paymentLocation.postalCode")),
                city = values.req("paymentLocation.city", values.str("paymentLocation.city")),
                district = values.req("paymentLocation.district", values.str("paymentLocation.district")),
            ),
        )
    },
    MethodForm("CRYPTO_OFFLINE", R.string.synqt_crypto, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
    )) { values ->
        PaymentMethodPayin.CryptoOfflineMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
        )
    },
    MethodForm("DUITNOW", R.string.synqt_duitnow, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account),
    )) { values ->
        PaymentMethodPayin.DuitNowMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
                accountNumber = values.str("account.accountNumber"),
            ),
        )
    },
    MethodForm("EWALLET", R.string.synqt_e_wallet, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.EWalletMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                )
            },
        )
    },
    MethodForm("FNB_DIRECT_EFT", R.string.synqt_fnb_pay, listOf(
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("passportNumber", R.string.synqt_passport_number, FieldKind.TEXT, required = false, hint = R.string.synqt_a_letter_followed_by_numbers, minLength = 8, maxLength = 9, pattern = "^[a-zA-Z][0-9]+\$", invalid = R.string.synqt_enter_a_letter_followed_by_7_or_8_numbers),
        FormField("said", R.string.synqt_south_african_id_number, FieldKind.NUMBER, required = false, hint = R.string.synqt_13_digits, minLength = 13, maxLength = 13, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_13_digits),
        FormField("customerName.firstName", R.string.synqt_first_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account),
    ), oneOfs = listOf(OneOf(R.string.synqt_how_do_you_want_to_confirm_it_s_you, listOf(Choice(R.string.synqt_south_african_id_number, listOf("said")), Choice(R.string.synqt_passport_number, listOf("passportNumber")))))) { values ->
        PaymentMethodPayin.FnbDirectEFTMethod(
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            passportNumber = values.str("passportNumber"),
            said = values.str("said"),
            customerName = PaymentMethodPayin.PersonName(
                firstName = values.req("customerName.firstName", values.str("customerName.firstName")),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
            ),
            account = PaymentMethodPayin.Account(
                accountNumber = values.req("account.accountNumber", values.str("account.accountNumber")),
            ),
        )
    },
    MethodForm("FPX", R.string.synqt_fpx_online_banking, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountType", R.string.synqt_account_type, FieldKind.CHOICE, required = true, section = R.string.synqt_bank_account, options = listOf("savings" to R.string.synqt_savings, "current" to R.string.synqt_current, "corporate" to R.string.synqt_corporate)),
    )) { values ->
        PaymentMethodPayin.FpxMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
                accountType = values.req("account.accountType", values.str("account.accountType")),
            ),
        )
    },
    MethodForm("IMPS", R.string.synqt_imps_bank_transfer, listOf(
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account),
    )) { values ->
        PaymentMethodPayin.IMPSMethod(
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
                accountNumber = values.str("account.accountNumber"),
            ),
        )
    },
    MethodForm("MOBILE_MONEY", R.string.synqt_mobile_money, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = true, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.MobileMoneyMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.req("phoneNumber", values.str("phoneNumber")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
        )
    },
    MethodForm("MOMOPAY", R.string.synqt_momopay, listOf(
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
    )) { values ->
        PaymentMethodPayin.MomoPayMethod(
            emailAddress = values.str("emailAddress"),
        )
    },
    MethodForm("NEDBANK_DIRECT_EFT", R.string.synqt_nedbank_pay, listOf(
        FormField("said", R.string.synqt_south_african_id_number, FieldKind.NUMBER, required = true, hint = R.string.synqt_13_digits, minLength = 13, maxLength = 13, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_13_digits),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    )) { values ->
        PaymentMethodPayin.NedbankDirectEFTMethod(
            said = values.req("said", values.str("said")),
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.str("customerName.firstName"),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("NETBANKING", R.string.synqt_net_banking, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
    )) { values ->
        PaymentMethodPayin.NetBankingMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
        )
    },
    MethodForm("OFFLINE", R.string.synqt_pay_offline, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("productId", R.string.synqt_product_reference_optional, FieldKind.TEXT, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$"),
        FormField("taxId", R.string.synqt_tax_number_optional, FieldKind.TEXT, required = false, hint = R.string.synqt_8_to_18_characters, minLength = 8, maxLength = 18, invalid = R.string.synqt_enter_8_to_18_characters),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    )) { values ->
        PaymentMethodPayin.OfflineMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            productId = values.str("productId"),
            taxId = values.str("taxId"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.req("customerName.firstName", values.str("customerName.firstName")),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("ONLINE", R.string.synqt_online_banking, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account),
        FormField("account.accountType", R.string.synqt_account_type, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.OnlineMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
                accountNumber = values.req("account.accountNumber", values.str("account.accountNumber")),
                accountType = values.req("account.accountType", values.str("account.accountType")),
            ),
        )
    },
    MethodForm("OTT_VOUCHER", R.string.synqt_ott_voucher, listOf(
        FormField("voucherPin", R.string.synqt_voucher_pin, FieldKind.TEXT, required = true, hint = R.string.synqt_the_12_digit_pin_on_your_voucher),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = true, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    )) { values ->
        PaymentMethodPayin.OttVoucherMethod(
            voucherPin = values.req("voucherPin", values.str("voucherPin")),
            phoneNumber = values.req("phoneNumber", values.str("phoneNumber")),
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.req("customerName.firstName", values.str("customerName.firstName")),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("P2A_V2", R.string.synqt_bank_transfer, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
    )) { values ->
        PaymentMethodPayin.P2AV2Method(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
        )
    },
    MethodForm("PAYSHAP", R.string.synqt_payshap, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = false, placeholder = R.string.synqt_0821234567, maxLength = 12, pattern = "^0", invalid = R.string.synqt_enter_up_to_12_characters_starting_with_0),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("passportNumber", R.string.synqt_passport_number_optional, FieldKind.TEXT, required = false, hint = R.string.synqt_a_letter_followed_by_numbers, minLength = 8, maxLength = 9, pattern = "^[a-zA-Z][0-9]+\$", invalid = R.string.synqt_enter_a_letter_followed_by_7_or_8_numbers),
        FormField("said", R.string.synqt_south_african_id_number_optional, FieldKind.NUMBER, required = false, hint = R.string.synqt_13_digits, minLength = 13, maxLength = 13, pattern = "^[0-9]*\$", invalid = R.string.synqt_enter_13_digits),
        FormField("externalProviderCode", R.string.synqt_provider, FieldKind.PROVIDER, required = false),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = false, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.NUMBER, required = false, hint = R.string.synqt_numbers_only, pattern = "^[0-9]*\$", invalid = R.string.synqt_use_numbers_only),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    ), oneOfs = listOf(OneOf(R.string.synqt_how_do_you_want_to_pay, listOf(Choice(R.string.synqt_bank_account, listOf("account.accountNumber", "account.accountName"), required = listOf("account.accountNumber", "account.accountName")), Choice(R.string.synqt_mobile_number, listOf("phoneNumber", "externalProviderCode"), required = listOf("phoneNumber", "externalProviderCode")))), OneOf(R.string.synqt_which_id_do_you_want_to_give, listOf(Choice(R.string.synqt_south_african_id_number, listOf("said"), required = emptyList()), Choice(R.string.synqt_passport_number, listOf("passportNumber"), required = emptyList()))))) { values ->
        PaymentMethodPayin.PayShapMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            phoneNumber = values.str("phoneNumber"),
            emailAddress = values.str("emailAddress"),
            passportNumber = values.str("passportNumber"),
            said = values.str("said"),
            externalProviderCode = values.str("externalProviderCode"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.str("customerName.firstName"),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("PAY_ME", R.string.synqt_payme, listOf(
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account),
    )) { values ->
        PaymentMethodPayin.PayMeMethod(
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
                accountNumber = values.str("account.accountNumber"),
            ),
        )
    },
    MethodForm("PIXQR", R.string.synqt_pix, listOf(
        FormField("taxId", R.string.synqt_cpf_or_cnpj, FieldKind.TEXT, required = true, hint = R.string.synqt_14_characters, minLength = 14, maxLength = 14, invalid = R.string.synqt_enter_your_14_character_cpf_or_cnpj),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.PixQRMethod(
            taxId = values.req("taxId", values.str("taxId")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
        )
    },
    MethodForm("PROMPTPAY", R.string.synqt_promptpay, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account),
    )) { values ->
        PaymentMethodPayin.PromptPayMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.str("account.accountName"),
                accountNumber = values.req("account.accountNumber", values.str("account.accountNumber")),
            ),
        )
    },
    MethodForm("PSE", R.string.synqt_pse, listOf(
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = true, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.PseMethod(
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            phoneNumber = values.req("phoneNumber", values.str("phoneNumber")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
        )
    },
    MethodForm("QRISPAY", R.string.synqt_qris, listOf(
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
    )) { values ->
        PaymentMethodPayin.QrisPayMethod(
            emailAddress = values.str("emailAddress"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
        )
    },
    MethodForm("QRPH", R.string.synqt_qr_ph, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
    )) { values ->
        PaymentMethodPayin.QrPhMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
        )
    },
    MethodForm("SNAPSCAN_QR", R.string.synqt_snapscan, listOf(
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("customerName.titleBefore", R.string.synqt_title_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.firstName", R.string.synqt_first_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.middleName", R.string.synqt_middle_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.titleAfter", R.string.synqt_name_suffix_optional, FieldKind.TEXT, required = false, section = R.string.synqt_your_name),
    )) { values ->
        PaymentMethodPayin.SnapScanQrMethod(
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            customerName = PaymentMethodPayin.PersonName(
                titleBefore = values.str("customerName.titleBefore"),
                firstName = values.req("customerName.firstName", values.str("customerName.firstName")),
                middleName = values.str("customerName.middleName"),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
                titleAfter = values.str("customerName.titleAfter"),
            ),
        )
    },
    MethodForm("UPICOLLECT", R.string.synqt_upi, listOf(
        FormField("upiId", R.string.synqt_upi_id, FieldKind.TEXT, required = true, placeholder = R.string.synqt_name_bank, maxLength = 100, pattern = "[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9_\\.]+", invalid = R.string.synqt_enter_a_upi_id_like_name_bank),
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number, FieldKind.PHONE, required = true, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name, FieldKind.TEXT, required = true, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.UpiCollectMethod(
            upiId = values.req("upiId", values.str("upiId")),
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            phoneNumber = values.req("phoneNumber", values.str("phoneNumber")),
            account = PaymentMethodPayin.Account(
                accountName = values.req("account.accountName", values.str("account.accountName")),
            ),
        )
    },
    MethodForm("UPIID", R.string.synqt_upi_id, listOf(
        FormField("upiId", R.string.synqt_upi_id, FieldKind.TEXT, required = true, placeholder = R.string.synqt_name_bank, maxLength = 100, pattern = "[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9_\\.]+", invalid = R.string.synqt_enter_a_upi_id_like_name_bank),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
    )) { values ->
        PaymentMethodPayin.UpiIdMethod(
            upiId = values.req("upiId", values.str("upiId")),
            emailAddress = values.str("emailAddress"),
        )
    },
    MethodForm("UPIQR", R.string.synqt_upi_qr, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("upiId", R.string.synqt_upi_id_optional, FieldKind.TEXT, required = false, placeholder = R.string.synqt_name_bank, maxLength = 100, pattern = "[a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9_\\.]+", invalid = R.string.synqt_enter_a_upi_id_like_name_bank),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
    )) { values ->
        PaymentMethodPayin.UpiQRMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            upiId = values.str("upiId"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                )
            },
        )
    },
    MethodForm("VAPAY", R.string.synqt_virtual_account, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider_optional, FieldKind.OPERATOR, required = false, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("phoneNumber", R.string.synqt_mobile_number_optional, FieldKind.PHONE, required = false, placeholder = R.string.synqt_27821234567, minLength = 7, maxLength = 16, pattern = "^\\+?[0-9]+\$", invalid = R.string.synqt_enter_the_number_with_your_country_code_and_no_s),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account_optional),
        FormField("paymentLocation.postalCode", R.string.synqt_postal_code, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
        FormField("paymentLocation.city", R.string.synqt_city, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
        FormField("paymentLocation.district", R.string.synqt_district, FieldKind.TEXT, required = true, section = R.string.synqt_where_you_are_paying),
    )) { values ->
        PaymentMethodPayin.VaPayMethod(
            paymentOperatorCode = values.str("paymentOperatorCode"),
            emailAddress = values.str("emailAddress"),
            phoneNumber = values.str("phoneNumber"),
            account = values.group("account") {
                PaymentMethodPayin.Account(
                    accountName = values.str("account.accountName"),
                    accountNumber = values.str("account.accountNumber"),
                )
            },
            paymentLocation = PaymentMethodPayin.PaymentLocation(
                postalCode = values.req("paymentLocation.postalCode", values.str("paymentLocation.postalCode")),
                city = values.req("paymentLocation.city", values.str("paymentLocation.city")),
                district = values.req("paymentLocation.district", values.str("paymentLocation.district")),
            ),
        )
    },
    MethodForm("VAPAY_VERIF", R.string.synqt_verified_virtual_account, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("account.accountName", R.string.synqt_account_holder_name_optional, FieldKind.TEXT, required = false, section = R.string.synqt_bank_account, maxLength = 64, pattern = "^(\\p{L}\\p{M}*?|[\\d, ;:_'\"!%=@#&\\-\\.\\?\\\$\\(\\)\\+\\*\\|])*\$", invalid = R.string.synqt_use_letters_numbers_spaces_and_common_punctuatio),
        FormField("account.accountNumber", R.string.synqt_account_number, FieldKind.NUMBER, required = true, hint = R.string.synqt_numbers_only, section = R.string.synqt_bank_account, pattern = "^[0-9-]*\$", invalid = R.string.synqt_use_numbers_only),
    )) { values ->
        PaymentMethodPayin.VaPayVerifMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
            account = PaymentMethodPayin.Account(
                accountName = values.str("account.accountName"),
                accountNumber = values.req("account.accountNumber", values.str("account.accountNumber")),
            ),
        )
    },
    MethodForm("VIETQR", R.string.synqt_vietqr, listOf(
        FormField("paymentOperatorCode", R.string.synqt_bank_or_provider, FieldKind.OPERATOR, required = true, maxLength = 30, pattern = "^[A-Za-z0-9_-]+\$", invalid = R.string.synqt_use_letters_numbers_and_only),
        FormField("emailAddress", R.string.synqt_email_address_optional, FieldKind.EMAIL, required = false, invalid = R.string.synqt_enter_a_valid_email_address),
    )) { values ->
        PaymentMethodPayin.VietQRMethod(
            paymentOperatorCode = values.req("paymentOperatorCode", values.str("paymentOperatorCode")),
            emailAddress = values.str("emailAddress"),
        )
    },
    MethodForm("ZAPPER_QR", R.string.synqt_zapper, listOf(
        FormField("emailAddress", R.string.synqt_email_address, FieldKind.EMAIL, required = true, invalid = R.string.synqt_enter_a_valid_email_address),
        FormField("customerName.firstName", R.string.synqt_first_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
        FormField("customerName.lastName", R.string.synqt_last_name, FieldKind.TEXT, required = true, section = R.string.synqt_your_name),
    )) { values ->
        PaymentMethodPayin.ZapperQrMethod(
            emailAddress = values.req("emailAddress", values.str("emailAddress")),
            customerName = PaymentMethodPayin.PersonName(
                firstName = values.req("customerName.firstName", values.str("customerName.firstName")),
                lastName = values.req("customerName.lastName", values.str("customerName.lastName")),
            ),
        )
    },
)

/** Payer-facing text for each of SYNQT's failure reason codes, whose own descriptions are written for the merchant. */
internal val FAILURE_MESSAGES: Map<String, Int> = mapOf(
    "SYSTEM" to R.string.synqt_failure_payment_process_error,
    "PAYMENT_PROCESS_ERROR" to R.string.synqt_failure_payment_process_error,
    "PAYMENT_METHOD_ERROR" to R.string.synqt_failure_payment_process_error,
    "ENTITY_EXPIRED" to R.string.synqt_failure_expired,
    "CUSTOMER_VERIFICATION_INACTIVITY" to R.string.synqt_failure_expired,
    "DIFFERENT_AMOUNT_CONFIRMED" to R.string.synqt_failure_amount_mismatch,
    "INCORRECT_CURRENCY" to R.string.synqt_failure_amount_mismatch,
    "CURRENCY_NOT_SUPPORTED" to R.string.synqt_failure_method_unavailable,
    "CURRENCY_PRECISION_EXCEEDED" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_METHOD_NOT_FOUND" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_CHANNEL_NO_ACTIVE_FOUND" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_CHANNEL_NO_OPENED_FOUND" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_CHANNEL_NO_SEGMENT_FOUND" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_CHANNEL_AMOUNT_LIMITS" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_CHANNEL_DAILY_LIMITS" to R.string.synqt_failure_method_unavailable,
    "PAYMENT_OPERATOR_NOT_FOUND" to R.string.synqt_failure_bank_unavailable,
    "PAYMENT_OPERATOR_INVALID" to R.string.synqt_failure_bank_unavailable,
    "PAYMENT_OPERATOR_UNAVAILABLE" to R.string.synqt_failure_bank_unavailable,
    "IP_DENIED" to R.string.synqt_failure_security,
    "SECURITY_CHECK_FAILED" to R.string.synqt_failure_security,
    "BALANCE_INSUFFICIENT" to R.string.synqt_failure_balance,
    "INVALID_ACCOUNT_NUMBER" to R.string.synqt_failure_account_number,
    "INVALID_ACCOUNT_NAME" to R.string.synqt_failure_account_name,
    "NAME_SIMILARITY_CHECK_FAILED" to R.string.synqt_failure_account_name,
    "INVALID_PHONE_NUMBER" to R.string.synqt_failure_phone_number,
    "INVALID_ID_NUMBER" to R.string.synqt_failure_id_number,
    "INVALID_IFSC" to R.string.synqt_failure_ifsc,
    "CUSTOMER_VERIFICATION_REJECTED" to R.string.synqt_failure_declined,
    "CUSTOMER_VERIFICATION_FAILED" to R.string.synqt_failure_not_verified,
    "ACCOUNT_TYPE_INELIGIBLE" to R.string.synqt_failure_account_ineligible,
    "FEATURE_UNAVAILABLE" to R.string.synqt_failure_feature_unavailable,
    "CUSTOMER_ACCOUNT_LIMIT_EXCEEDED" to R.string.synqt_failure_account_limit,
    "CUSTOMER_NOT_REGISTERED" to R.string.synqt_failure_not_registered,
    "VOUCHER_INELIGIBLE" to R.string.synqt_failure_voucher,
)
