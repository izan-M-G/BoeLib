import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.izanMG.boe.internal.dto.RespuestaBoeDto;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class RespuestaBoeDtoTest {


    @Test
    public void deserializarElStatus()throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

        InputStream is = getClass().getResourceAsStream("/fixtures/HappyPath.json");
        RespuestaBoeDto respuesta = mapper.readValue(is, RespuestaBoeDto.class);

        assertEquals("200",respuesta.status().code());
    }



}
