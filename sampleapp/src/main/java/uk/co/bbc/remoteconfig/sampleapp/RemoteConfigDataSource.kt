package uk.co.bbc.remoteconfig.sampleapp

class RemoteConfigDataSource: () -> Result<ByteArray> {

    private var count = 0

    override fun invoke(): Result<ByteArray> {
        val rem = (count / 10).rem(5)
        val json = when(rem) {
            0 -> {
                """
                {
                    "killed": false,
                    "retired": false,
                    "active_config": {
                        "active_message": "Hello, app is ALIVE! $count"
                    }
                }
                """
            }
            1 -> {
                """
                {
                    "killed": false,
                    "retired": true,
                    "retired_config": {
                        "retired_message": "Goodbye, app is RETIRED! $count"
                    }
                }
                """
            }
            2 -> {
                """
                invalid json
                """
            }
            3 -> {
                """
                {
                    "valid": "json"
                }
                """
            }
            else -> {
                """
                {
                    "killed": true
                }
                """
            }
        }

        count += 1

        return Result.success(json.encodeToByteArray())
    }
}