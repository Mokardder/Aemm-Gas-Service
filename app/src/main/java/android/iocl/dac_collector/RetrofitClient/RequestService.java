package android.iocl.dac_collector.RetrofitClient;

import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.BaseUpdateResponse;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;

import android.iocl.dac_collector.ModelData.FCM_Update;
import android.iocl.dac_collector.ModelData.SubsidyRequest;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_consumer;
import android.iocl.dac_collector.ModelData.update_dac_collect;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RequestService {
     String KEY_XR = "AKfycbzCu98bfvgb8uBqRIWkiZ-VAz2H7ZANSRLOTZucubIOkJ8ip3Rp6U3CXSAZNZw9VdMJ";
     String KEY_DAC_COLLECTOR = "AKfycbx7exruxsEFMMyzA_Y-GjH7jQtkCUW6PdrOjxTVpRsxi6pP4Yfc8mz_JyArY4hs_vFH";


    @POST("/macros/s/" + KEY_XR + "/exec")
    Call<DAC_Collector_Base> search_customer (@Body search_consumer body);

    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<BaseUpdateResponse> update_user (@Body update_consumer body);
    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<DAC_Collector_Base> check_update (@Body check_update body) ;
    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<DAC_Collector_Base> update_dac_collector (@Body update_dac_collect body) ;
    @POST("/macros/s/" + KEY_DAC_COLLECTOR + "/exec")
    Call<DAC_Collector_Base> requestSubsidyDetails (@Body SubsidyRequest body) ;

}
