package android.iocl.dac_collector.ModelData

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

class Payload {
}

data class dacPayload(
    val dac: String = "",        // Default value for dac
    val cashmemo: String = "",    // Default value for cashmemo
    val name: String = "",    // Default value for cashmemo
    val consID: String = "",    // Default value for cashmemo
    val timeStamp: String = ""    // Default value for timeStamp

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
    val type: String?,
    val find: String?,
)
data class check_update(
    val type: String?
)

data class appUpdateDesc(
    val desc: String?,
    val numbering: String?,
)
data class RegexModel(
    val regex: String,
    val captures: List<String>,
    val id: String
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

// Nested data model
data class AppUpdate(
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("app_version_code") val appVersionCode: String,
    @SerializedName("update_description") val updateDescription: String,
    @SerializedName("url") val url: String
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