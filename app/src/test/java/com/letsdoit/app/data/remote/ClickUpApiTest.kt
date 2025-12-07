package com.letsdoit.app.data.remote

import com.letsdoit.app.data.remote.dto.ClickUpTaskDto
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ClickUpApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: ClickUpApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClickUpApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getTask parses JSON correctly`() = runTest {
        val jsonResponse = """
            {
                "id": "123",
                "name": "Test Task",
                "text_content": "This is a description",
                "status": {
                    "status": "Open",
                    "color": "#000000",
                    "type": "open"
                },
                "due_date": "1700000000000",
                "priority": {
                    "id": "1",
                    "priority": "high",
                    "color": "#ff0000",
                    "orderindex": "1"
                },
                "date_created": "1700000000000",
                "date_updated": "1700000000000",
                "list": {
                    "id": "list_123"
                }
            }
        """

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse))

        val result = api.getTask("123")

        assertEquals("123", result.id)
        assertEquals("Test Task", result.name)
        assertEquals("This is a description", result.textContent)
        assertEquals("Open", result.status.status)
        assertEquals("high", result.priority?.priority)
        assertEquals("list_123", result.list.id)
    }
}
