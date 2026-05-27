package com.angelotacoj.self_adaptive_health_app.core.data

import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
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
        accessCredentials = AccessCredentials(
            userCode = "PACIENTE01",
            simulatedPin = "1234"
        ),
        appointment = Appointment(
            title = "Chequeo general (Simulado)",
            date = "15 de junio",
            time = "10:30 AM",
            instruction = "Traiga su documento de identidad. (Dato ficticio)"
        ),
        appointmentOptions = listOf(
            Appointment("Revisión de medicamentos (Simulada)", "10 de junio", "2:00 PM", "Traiga la lista de medicamentos."),
            Appointment("Chequeo general (Simulado)", "15 de junio", "10:30 AM", "Traiga su documento de identidad. (Dato ficticio)"),
            Appointment("Evaluación de visión (Simulada)", "21 de junio", "11:15 AM", "Traiga sus lentes si los usa.")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de bienestar (Simulado)",
            value = 7
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio de vitamina ficticio",
            time = "8:00 AM",
            frequency = "Todos los días"
        )
    )

    private val setB = FakeHealthDataSet(
        id = "Conjunto B",
        accessCredentials = AccessCredentials(
            userCode = "PACIENTE02",
            simulatedPin = "5678"
        ),
        appointment = Appointment(
            title = "Control preventivo (Ficticio)",
            date = "18 de junio",
            time = "9:45 AM",
            instruction = "Llegue 10 minutos antes. (Dato simulado)"
        ),
        appointmentOptions = listOf(
            Appointment("Orientación nutricional (Ficticia)", "12 de junio", "4:30 PM", "Traiga un registro de comidas si lo tiene."),
            Appointment("Control preventivo (Ficticio)", "18 de junio", "9:45 AM", "Llegue 10 minutos antes. (Dato simulado)"),
            Appointment("Revisión de resultados (Simulada)", "24 de junio", "8:20 AM", "Traiga su comprobante de cita.")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de energía (Simulado)",
            value = 6
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio de hidratación ficticio",
            time = "7:30 PM",
            frequency = "De lunes a viernes"
        )
    )
}
