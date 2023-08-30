package org.mifos.pheevouchermanagementsystem.api.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mifos.pheevouchermanagementsystem.data.RequestDTO;
import org.mifos.pheevouchermanagementsystem.data.ResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.concurrent.ExecutionException;

public interface CreateVoucherApi {
    @PostMapping("/vouchers")
    ResponseEntity<ResponseDTO> createVouchers(@RequestHeader(value="X-CallbackURL") String callbackURL, @RequestHeader(value = "X-Registering-Institution-ID") String registeringInstitutionId,
                                               @RequestBody RequestDTO requestBody) throws ExecutionException, InterruptedException, JsonProcessingException;
}
