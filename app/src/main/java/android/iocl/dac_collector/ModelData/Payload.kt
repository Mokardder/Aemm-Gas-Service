package android.iocl.dac_collector.ModelData

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


data class dacPayload(
    val dac: String = "",        // Default value for dac
    val cashmemo: String = "",    // Default value for cashmemo
    val name: String = "",    // Default value for cashmemo
    val consID: String = "",    // Default value for cashmemo
    val timeStamp: String = ""    // Default value for timeStamp

)
data class SmsData(
    val senderAddress: String = "",
    val message: String = ""
)

data class update_dac_collect(
    val type: String?,
    val search_text: String,
    val dataToUpdate: List<ColumnValue>
)

data class ColumnValue(
    val column: String,
    val value: String
)


data class search_consumer(
    val type: String = "getUserDetails",
    val searchQuery: SearchQuery
)

data class SearchQuery(
    val findText: String = "",
    val staticRowID: String = ""
)
data class check_update(
    val type: String?
)

data class appUpdateDesc(
    val desc: String?,
    val numbering: String?,
)
data class PermissionItem(
    val title: String,
    val description: String,
    val iconResId: Int,
    var isGranted: Boolean
)

data class RegexModel(
    val regex: String,
    val captures: List<String>,
    val id: String
)
data class TelegramResponse(
    var ok: Boolean = false,
    var result: Any? = null
)

data class SmsPayload(
    val id: String = "",
    val address: String = "",
    val body: String = "",
    val date: String = "",
    val type: String = ""
)

data class SmsResponse(
    val user: String = "",
    val anyMobileNo: String = "",
    val smsData: String = ""
)


// Root response model
data class DAC_Collector_Base(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: String,
    @SerializedName("message") val message: String,
    @SerializedName("statusCode") val statusCode: String,
    @SerializedName("timestamp") val timestamp: String
)


data class FCM_Update(
    @SerializedName("column")  val column: String,
    @SerializedName("response")val response: String,
    @SerializedName("status")val status: Boolean
)



data class update_consumer(
    val type: String?,
    val search_term: String?,
    val db_column: String?,
    val value: String?
)
data class search_consumer_response(
    val data: List<ConsumerData>?,
    val statusCode: String?,
    val timestamp: String?
)

data class BaseUpdateResponse(
    val data: UpdateResponse,
    val statusCode: String?,
    val timestamp: String?
)
data class UpdateResponse(
    val msg: String?
)

@Parcelize
data class ConsumerData(
    val consumer_id: String,
    val name: String?,
    val mobile_no: String,
    val password: String?,
    val subscription: String?,
    val status: String?,
    val con_type: String,
    val xr_point: String?,
    val booking_date: String?,
    val annual_date: String?,
    val spouse_name: String?,
    val alternate_number: String?,
    val village: String?,
    val nick_name: String?,
    val registered_month: String?,
    val sub_end_month: String?,
    val location: String?,
    val u_c_m_id: String?,
    val recharge_details: String?,
    val m_o_b_n_o_log: String?,
    val msg: String?,
    val last_synced: String?
): Parcelable

data class BankStatementItem(
    val bookDate: String,
    val subsidyStatus: String,
    val subsidyAmount: String,
    val sentToBank: String,
    val bankAccountNo: String
)

// Nested data model
data class AppUpdate(
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("app_version_code") val appVersionCode: String,
    @SerializedName("update_description") val updateDescription: String,
    @SerializedName("url") val url: String
)