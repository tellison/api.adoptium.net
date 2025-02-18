package net.adoptium.api.v3.routes

import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Schema(hidden = true)
@Produces(MediaType.TEXT_PLAIN)
class SwaggerUiRoute {
    @GET
    @Schema(hidden = true)
    @Path("/{path:openapi|swagger-ui}")
    @Operation(hidden = true)
    fun redirectOpenAPIPaths(@PathParam("path") path: String): Response {
        return Response
            .status(Response.Status.FOUND)
            .location(URI("/q/$path"))
            .build()
    }
}
