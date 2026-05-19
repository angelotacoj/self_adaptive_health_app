package com.angelotacoj.self_adaptive_health_app.core.data

import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord

class FakeHealthDataSource {
    fun getDataSet(group: ExperimentGroup): FakeHealthDataSet {
        return when (group) {
            ExperimentGroup.GroupA -> setA
            ExperimentGroup.GroupB -> setB
        }
    }

    private val setA = FakeHealthDataSet(
        id = "Conjunto A",
        appointment = Appointment(
            title = "Chequeo general",
            date = "15 de junio",
            time = "10:30 AM",
            instruction = "Traiga su documento de identidad."
        ),
        appointmentOptions = listOf(
            Appointment("Revisión de medicamentos", "10 de junio", "2:00 PM", "Traiga la lista de medicamentos."),
            Appointment("Chequeo general", "15 de junio", "10:30 AM", "Traiga su documento de identidad."),
            Appointment("Evaluación de visión", "21 de junio", "11:15 AM", "Traiga sus lentes si los usa.")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de bienestar",
            value = 7
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio ficticio de vitamina",
            time = "8:00 AM",
            frequency = "Todos los días"
        )
    )

    private val setB = FakeHealthDataSet(
        id = "Conjunto B",
        appointment = Appointment(
            title = "Control preventivo",
            date = "18 de junio",
            time = "9:45 AM",
            instruction = "Llegue 10 minutos antes."
        ),
        appointmentOptions = listOf(
            Appointment("Orientación nutricional", "12 de junio", "4:30 PM", "Traiga un registro de comidas si lo tiene."),
            Appointment("Control preventivo", "18 de junio", "9:45 AM", "Llegue 10 minutos antes."),
            Appointment("Revisión de resultados", "24 de junio", "8:20 AM", "Traiga su comprobante de cita.")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de energía",
            value = 6
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio de hidratación",
            time = "7:30 PM",
            frequency = "De lunes a viernes"
        )
    )
}
