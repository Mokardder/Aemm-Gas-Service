package android.iocl.dac_collector.RetrofitClient;

import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.BaseUpdateResponse;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;

import android.iocl.dac_collector.ModelData.FCM_Update;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_consumer;
import android.iocl.dac_collector.ModelData.update_dac_collect;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RequestService {
     String KEY_XR = "AKfycbxMd0_pM82ivia2kkuSh-ryyyG33x3ICcyONPb-FtN4dkF5OzKuvVhmQSEPlmXupN1d";
     String KEY_DAC_COLLECTOR = "AKfycbzLj-JthmIpU8BdmCgKKtHiPFBze9SYYLXZWQT2TiMlN2fwEpHhTtNX9qbjYYi4T0yE";


    @POST("/macros/s/" + KEY_XR + "/exec")
    Call<search_consumer_response> search_customer (@Body search_consumer body);

    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<BaseUpdateResponse> update_user (@Body update_consumer body);
    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<DAC_Collector_Base> check_update (@Body check_update body) ;
    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<DAC_Collector_Base> update_dac_collector (@Body update_dac_collect body) ;
}
