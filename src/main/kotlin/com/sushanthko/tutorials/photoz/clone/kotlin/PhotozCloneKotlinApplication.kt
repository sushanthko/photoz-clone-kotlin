package com.sushanthko.tutorials.photoz.clone.kotlin

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.http.*
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import kotlin.jvm.optionals.getOrNull

@SpringBootApplication
class PhotozCloneKotlinApplication

fun main(args: Array<String>) {
    runApplication<PhotozCloneKotlinApplication>(*args)
}

@RestController
class PhotozController(val photozService: PhotozService) {

    @GetMapping
    fun hello(): String {
        return "Hello World!"
    }

    @GetMapping("photoz")
    fun get(): Iterable<Photo> = photozService.get()

    @GetMapping("photoz/{id}")
    fun get(@PathVariable id: Int): Photo {
        return photozService.get(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")
    }

    @DeleteMapping("photoz/{id}")
    fun delete(@PathVariable id: Int) = photozService.remove(id)

    @PostMapping("photoz")
    fun create(@RequestPart("data") file: MultipartFile): Photo {
        return photozService.save(file.originalFilename, file.contentType, file.bytes)
    }
}

@Service
class PhotozService(val photozRepository: PhotozRepository) {

    fun get(): Iterable<Photo> = photozRepository.findAll()

    fun get(id: Int): Photo? = photozRepository.findById(id).getOrNull()

    fun remove(id: Int) = photozRepository.deleteById(id)

    fun save(fileName: String?, contentType: String?, data: ByteArray): Photo {
        val photo = Photo(null, fileName!!, contentType, data)
        return photozRepository.save(photo)
    }
}

@Repository
interface PhotozRepository : CrudRepository<Photo, Int>

@Table("PHOTOZ")
data class Photo(
    @Id val id: Int?,
    @NotEmpty val fileName: String,
    val contentType: String?,
    @JsonIgnore val data: ByteArray
)

@RestController
class DownloadController(val photozService: PhotozService) {

    @GetMapping("/download/{id}")
    fun download(@PathVariable id: Int): ResponseEntity<ByteArray> {
        val photo = photozService.get(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found")

        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = photo.contentType?.let(MediaType::valueOf)

        httpHeaders.contentDisposition = ContentDisposition
            .builder("attachment")
            .filename(photo.fileName)
            .build()

        return ResponseEntity(photo.data, httpHeaders, HttpStatus.OK)
    }
}
